$(document).ready(function() {
  $('table tr:even').addClass("even");
  $('a.point').click(function() {
    var p = $(this).parent();
    var url = $(this).attr("href").split("=")[1];
    $.post("point",{title: url},function(data) {
      var data = jQuery.parseJSON(data);
      
      if(data.count  == -1) {
        $('a.point').fadeOut();
      } else {
        p.siblings().first().html(data.count);
      }
    })
    return false;
  });

  //select all the a tag with name equal to modal
  $('a[name=modal]').click(function(e) {
    e.preventDefault();
    var id = $(this).attr('href');
    var maskHeight = $(document).height();
    var maskWidth = $(window).width();
    $('#mask').css({'width':maskWidth,'height':maskHeight});
    $('#mask').fadeIn(1000);	
    $('#mask').fadeTo("slow",0.8);	
    var winH = $(window).height();
    var winW = $(window).width();
    $(id).css('top',  winH/2-$(id).height()/2);
    $(id).css('left', winW/2-$(id).width()/2);
    $(id).fadeIn(2000); 
  });
	
  $('.window .close').click(function (e) {
    e.preventDefault();
    $('#mask').hide();
    $('.window').hide();
  });		
	
  $('#mask').click(function () {
    $(this).hide();
    $('.window').hide();
  });			

  

  function loadnew() {
    $.ajax({ url: "/promise", 
           context: document.body, 
           success: function(data){
             if(data != 0) {
               $("#news tr:first").before(data);
               $("#news tr:first").hide().fadeIn(1000);
             }
           
             setTimeout(loadnew,10);}
           });
  }
  
  loadnew();



})
