//an edited version of http://twitter.com/javascripts/blogger.js

function twitterCallback(twitters) {
  var statusHTML = [];
  for (var i=0; i<twitters.length; i++){
    // ignore "reply" tweets
    if(twitters[i].in_reply_to_status_id) {
      continue;
    }
    var username = twitters[i].user.screen_name;
    var tweet = twitters[i];
    var status;
    if(tweet.retweeted_status) {
      status = 'RT <a href=\"http://twitter.com/'+tweet.retweeted_status.user.screen_name+'\">@'+tweet.retweeted_status.user.screen_name+"<\/a>: "+status_text(tweet.retweeted_status);
    } else {
      status = status_text(tweet);
    }

    statusHTML.push('<li><span>'+status+'</span> <a style="font-size:85%" href="http://twitter.com/'+username+'/statuses/'+twitters[i].id_str+'">'+relative_time(twitters[i].created_at)+'</a></li>');
  }
  document.getElementById('twitter_update_list').innerHTML = statusHTML.join('');
}

function status_text(tweet) {
  // amalgamate all the user_mentions and urls entities into one big array
  var entities = new Array();
  var urls = tweet.entities.urls;
  if(urls) {
    for(var i=0; i < urls.length; i++) {
      urls[i].kind = "url";
      entities.push(urls[i]);
    }
  }
  var user_mentions = tweet.entities.user_mentions;
  if(user_mentions) {
    for(var i=0; i < user_mentions.length; i++) {
      user_mentions[i].kind = "user";
      entities.push(user_mentions[i]);
    }
  }

  // sort by offset, so all the mentions are in left-to-right order
  entities.sort(sort_by_start_index);

  // now build up the text with the mentions at the right places
  var text = "";
  var last_index = 0;
  for(var i=0; i < entities.length; i++) {
    // append text up to the start of the mention
    text += tweet.text.substring(last_index, entities[i].indices[0]);
    // deal with the entity
    if(entities[i].kind == "url") {
      text += '<a href=\"' + entities[i].url + '">';
      // use a nice display_url if there is one
      if(entities[i].display_url) {
        text += entities[i].display_url;
      } else {
        text += entities[i].url;
      }
      text += '<\/a>';
    } else if(entities[i].kind == "user") {
      text += '<a href=\"http://twitter.com/' + entities[i].screen_name + '">' + tweet.text.substring(entities[i].indices[0], entities[i].indices[1]) + '<\/a>';
    }
    // continue from the end index of the mention
    last_index = entities[i].indices[1];
  }

  text += tweet.text.substring(last_index);
  return text;
}

function sort_by_start_index(a, b) {
  return a.indices[0] - b.indices[0];
}

function relative_time(time_value) {
  var values = time_value.split(" ");
  time_value = values[1] + " " + values[2] + ", " + values[5] + " " + values[3];
  var parsed_date = Date.parse(time_value);
  var relative_to = (arguments.length > 1) ? arguments[1] : new Date();
  var delta = parseInt((relative_to.getTime() - parsed_date) / 1000);
  delta = delta + (relative_to.getTimezoneOffset() * 60);

  if (delta < 60) {
    return 'less than a minute ago';
  } else if(delta < 120) {
    return 'about a minute ago';
  } else if(delta < (60*60)) {
    return (parseInt(delta / 60)).toString() + ' minutes ago';
  } else if(delta < (120*60)) {
    return 'about an hour ago';
  } else if(delta < (24*60*60)) {
    return 'about ' + (parseInt(delta / 3600)).toString() + ' hours ago';
  } else if(delta < (48*60*60)) {
    return '1 day ago';
  } else {
    return (parseInt(delta / 86400)).toString() + ' days ago';
  }
}
