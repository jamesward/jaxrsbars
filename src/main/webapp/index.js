$(function() {
    $("body").append('<h4>Bars:</h4>');
    $("body").append('<ul id="bars"></ul>');

    loadbars();

    $("body").append('<input id="bar"/>');
    $("body").append('<button id="submit">GO!</button>');


    $("#submit").click(addbar);
    $("#bar").keyup(function(key) {
        if (key.which == 13) {
            addbar();
        }
    });

});

function loadbars() {
    $.ajax("/bars", {
        contentType: "application/json",
        success: function(data) {
            $("#bars").children().remove();
            $.each(data, function(index, item) {
                $("#bars").append($("<li>").text(item.name));
            });
        }
    });
}

function addbar() {
    $.ajax({
        url: "/bar",
        type: 'post',
        dataType: 'json',
        contentType: 'application/json',
        data: JSON.stringify({name:$("#bar").val()}),
        success: loadbars
    });
}