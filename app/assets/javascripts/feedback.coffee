updateInfluence = () ->
  $(".username:not(:has(.influence))").each((i, elem) ->
    user = $(elem)
    $.getJSON("/" + user.text() + "/influence", (data) ->
      influence = $("<span class='influence'>")
      user.append(influence)
      if data.score
        influence.text(" (" + data.score + ")")
    )
  )

ws = new WebSocket("ws://localhost:9000/feed")

ws.onmessage = (event) ->
  feedback = JSON.parse(event.data)
  markup = $("<li>").append($("<div class='feedback'>").text(feedback.feedback))
    .append($("<div class='username'>").text(feedback.username))
  $(".all-feedback").prepend(markup)
  updateInfluence()

$(window).ready(() ->
  updateInfluence()
  $("form").submit((event) ->
    ws.send(JSON.stringify(
      username: $("#username").val()
      feedback: $("#feedback").val()
    ))
    $("#username").val("")
    $("#feedback").val("")
    false
  )
)