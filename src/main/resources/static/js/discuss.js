$(function(){
    $("#topBtn").click(setTop);
    $("#topBtnC").click(cancelTop);
    $("#wonderfulBtn").click(setWonderful);
    $("#wonderfulBtnC").click(cancelWonderful);
});
function deleteReply(btn,replyId){ //删除
    var othis = $(btn)
    var li = othis.parents('.reply-li');
    layer.confirm('确认删除该回复么？', function(index){
        layer.close(index);
        $.post('/comment/delete/', {
            commentId:replyId
        }, function(data){
            data = $.parseJSON(data);
            if(data.status === 0){
                layer.alert('删除成功！', {
                    icon: 1,
                    time: 10*1000,
                });
                li.remove();
            } else {
                layer.msg(data.msg);
            }
        });
    });
}

function replyTarget(btn, commentId,replyId) {
    var str1 = "#username-" + replyId;
    var username = $(str1).val();
    var str2 = "#reply-"+commentId;
    var input = $(str2);
    $(input).prop("value","回复"+username+":");
}
function replyList(btn, entityId) {
    var str = "#replylist-"+entityId;
    var spread = $(str);
    spread.slideToggle();
}
function favorite(btn, entityType, entityId,favoriteStatus) {
    $.post(
        CONTEXT_PATH + "/favorite",
        {"entityType":entityType,"entityId":entityId},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                if(data.favoriteStatus==0){
                    $(btn).children("b").removeClass("liked");
                    layer.alert('已取消收藏！', {
                        icon: 1,
                        time: 10*1000,
                    });
                }else{
                    $(btn).children("b").addClass("liked");
                    layer.alert('已收藏！', {
                        icon: 1,
                        time: 10*1000,
                    });
                }
            } else {
                layer.msg(data.msg,{shift: 6});
            }
        }
    );
}

function like(btn, entityType, entityId, entityUserId, postId) {
    $.post(
        CONTEXT_PATH + "/like",
        {"entityType":entityType,"entityId":entityId,"entityUserId":entityUserId,"postId":postId},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                $(btn).children("span").text(data.likeCount);
                if(data.likeStatus==1){
                    $(btn).children("i").text("已赞");
                    $(btn).children("i").addClass("liked");
                    $(btn).children("span").addClass("liked");
                    $(btn).children("b").addClass("liked");
                }else{
                    $(btn).children("i").text("赞");
                    $(btn).children("i").removeClass("liked");
                    $(btn).children("span").removeClass("liked");
                    $(btn).children("b").removeClass("liked");
                }
            } else {
                layer.msg(data.msg,{shift: 6});
            }
        }
    );
}


// 置顶
function setTop() {
    $.post(
        CONTEXT_PATH + "/discuss/top/1",
        {"id":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                window.location.reload()
            } else {
                layer.msg(data.msg,{shift: 6});
            }
        }
    );
}

// 取消置顶
function cancelTop() {
    $.post(
        CONTEXT_PATH + "/discuss/top/0",
        {"id":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                window.location.reload()
            } else {
                layer.msg(data.msg,{shift: 6});
            }
        }
    );
}

// 加精
function setWonderful() {
    $.post(
        CONTEXT_PATH + "/discuss/wonderful/1",
        {"id":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                window.location.reload()
            } else {
                layer.msg(data.msg,{shift: 6});
            }
        }
    );
}

// 取消加精
function cancelWonderful() {
    $.post(
        CONTEXT_PATH + "/discuss/wonderful/0",
        {"id":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                window.location.reload()
            } else {
                layer.msg(data.msg,{shift: 6});
            }
        }
    );
}


