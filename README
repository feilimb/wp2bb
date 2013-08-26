wp2bb
=====

A Wordpress post to BB forum thread migration tool.

I maintain a Wordpress blog (http://ronnach.wordpress.com), and often want to add a post I have created to a Forum I frequent.

This tool takes a Wordpress blog post in its code format (see example below) and performs the following:
  1) cleans out contents of an 'img' directory in the root of the application
  2) parses the post for images used
  3) download & resize performed on the images to all have the same width (currently 640px, note the width:height ratio is maintained)
  4) resized images uploaded anonymously to imgur.com
  5) original blog post code modified and dumped to console with ported forum code (using embedded links to uploaded imgur images)
  
For example:

Wordpress blog post (code taken from Wordpress "Edit Post" admin page):
==============================
My first paragraph.

[caption id="attachment_288" align="aligncenter" width="580"]<img class="size-large wp-image-288" alt="My First Image" src="http://ronnach.files.wordpress.com/image01.jpg?w=580" width="580" height="325" /> My First Caption[/caption]

My second paragraph.
<p style="text-align:center;"><img class=" wp-image-297 aligncenter" alt="" src="http://ronnach.files.wordpress.com/image02.png?w=580" width="348" height="392" /></p>
My third paragraph.
==============================

BB forum post generated (where image URLs below are uploaded and available on imgur):
==============================
My first paragraph.

[CENTER][IMG]http://i.imgur.com/abc.jpg[/IMG][/CENTER]

My second paragraph.
[CENTER][IMG]http://i.imgur.com/xyz.jpg[/IMG][/CENTER]
My third paragraph.
==============================

Notes:
* Supported image format extensions: jpg, png, gif.
* Transparency is maintained in PNG images, but not in GIF images.
* Currently not possible to specify width, image alignment, or name of text file containing wordpress post.
* Any 'caption' specified for an image in the Wordpress post is not ported to the BB forum post code.
