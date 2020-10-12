var wait = 60;
function time(btn) {
    if (wait == 0) {
        btn.removeAttribute("disabled");
        btn.className = "layui-btn";
        btn.innerHTML = "获取验证码";
        wait = 60;
    } else {
        btn.setAttribute("disabled", true);
        btn.className = "layui-btn layui-btn-disabled";
        btn.innerHTML = wait + "秒后重新获取";
        wait--;
        setTimeout(function () {
                time(btn);
            },
            1000)
    }
}
function verifycode() {
    var btn = document.getElementById("getverifycode");
    console.log(btn);
    time(btn);

    var email = $("#email").val();


    $.post(
        CONTEXT_PATH + "/forget/verifycode",
        {"email":email},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                layer.msg("发送验证码成功！")
            } else {
                layer.msg(data.msg,{shift: 6})
            }

        }
    );
}