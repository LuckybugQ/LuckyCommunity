$(function(){
	$(".follow-btn").click(follow);
});
var end = function(){
	window.location.reload()
};
function follow() {
	var btn = this;
	if($(btn).hasClass("layui-btn-warm")) {
		// 关注TA
		$.post(
			CONTEXT_PATH + "/follow",
			{"entityType":3,"entityId":$(btn).prev().val()},
			function(data) {
				data = $.parseJSON(data);
				if(data.code == 0) {
					layer.alert("已关注！", {
						icon: 1,
						time: 10*1000,
						end: end
					});
				} else {
					alert(data.msg);
				}
			}
		);
		//$(btn).text("取消关注").removeClass("layui-btn-warm").addClass("layui-btn-primary");
	} else {
		// 取消关注
		$.post(
			CONTEXT_PATH + "/unfollow",
			{"entityType":3,"entityId":$(btn).prev().val()},
			function(data) {
				data = $.parseJSON(data);
				if(data.code == 0) {
					layer.alert("已取消关注！", {
						icon: 1,
						time: 10*1000,
						end: end
					});
				} else {
					alert(data.msg);
				}
			}
		);
		//$(btn).text("关注TA").removeClass("layui-btn-primary").addClass("layui-btn-warm");
	}
}