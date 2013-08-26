package com.feilimb.wp2bb;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.imgscalr.Scalr;
import org.json.JSONException;
import org.json.JSONObject;

public class WP2BB
{
   private static final String API_KEY = "c5aa91da273676e";
   
   private static final Pattern PATTERN_IMG_EXTENSION = Pattern.compile("\\.jpg$|\\.png$|\\.gif$");

   private static final String NL = "\n";
   
   private static final String IMG_DIR_NAME = "img";
   
   /**
    * @param args
    */
   public static void main(String[] args)
   {
      WP2BB w = new WP2BB();
      w.start("foo.txt");
   }
   
   private void start(String filename)
   {
      Set<ImageInfo> imageInfos;
      File f = new File(".");
      String filePath = f.getAbsolutePath() + File.separator + filename;
	   StringBuilder sb = readFileIntoString(filePath);
	   
	   // clean out any previously created images
	   File dir = new File(f.getAbsolutePath() + File.separator + IMG_DIR_NAME);
	   if (dir.exists() && dir.isDirectory())
	   {
   	   for(File file: dir.listFiles())
   	   {
   	      file.delete();
         }
	   }
	   
	   // find image tags within the wordpress post and change the references 
	   // to them to temporary (cached) replacement strings
      imageInfos = parseAndReplaceImgTags(sb);
      
      // parse out the actual image http URLs
      parseImgURLs(imageInfos);
      
      // resize those images to images saved on local disk
      getImagesAndResizeLocally(imageInfos);
      
      // upload the resized images to imgur, and cache the imgur URLs for each image
      uploadToImgur(imageInfos);
      
      // get the source to use for a forum post, with imgur images embedded
      String bbPost = getBBPost(sb, imageInfos);
      
      System.out.println(">>>=======================");
      System.out.println(bbPost);
      System.out.println(">>>=======================");
   }
   
   private Set<ImageInfo> parseAndReplaceImgTags(StringBuilder sb) 
   {
      Set<ImageInfo> imageInfos = new LinkedHashSet<ImageInfo>();
      final String replacementPrefix = "imageInfo";
      Pattern p = Pattern.compile("<img.*/>");
      Pattern p0 = Pattern.compile("\\[caption.*/caption]");
      Matcher m0 = p0.matcher(sb);
      int index = 0;
      int idx = 0;
      while (m0.find(idx))
      {
         String s = m0.group(0);
         Matcher m1 = p.matcher(s);
         if (m1.find())
         {
            String imgtag = m1.group(0);
            ImageInfo i = new ImageInfo();
            i.setWordpressImgTag(imgtag);
            String r = replacementPrefix + String.format("%03d", index++);
            i.setTempReplacement(r);
            imageInfos.add(i);
            sb.replace(m0.start(), m0.end(), r);
            idx = m0.start() + 1;
         }
      }      
      
      Matcher m = p.matcher(sb);
      idx = 0;
      while (m.find(idx))
      {
   	   String s = m.group(0);
   	   ImageInfo i = new ImageInfo();
   	   i.setWordpressImgTag(s);
   	   String r = replacementPrefix + String.format("%03d", index++);
   	   i.setTempReplacement(r);
   	   imageInfos.add(i);
   	   sb.replace(m.start(), m.end(), r);
   	   idx = m.start() + 1;
      }
      
      return imageInfos;
   }

   private void parseImgURLs(Set<ImageInfo> imageInfos) 
   {
      Pattern p = Pattern.compile("src=\"(\\S*)\"");
      for (ImageInfo i : imageInfos)
      {
   	   Matcher m = p.matcher(i.getWordpressImgTag());
   	   if (m.find())
   	   {
   		   String match = m.group(1);
   		   if (match.contains("?"))
   		   {
   			   match = match.substring(0, match.indexOf("?"));
   		   }
   		   i.setWordpressURL(match);
   	   }
      }
   }

   private void getImagesAndResizeLocally(Set<ImageInfo> imageInfos)
   {
      int index = 0;
      for (ImageInfo i : imageInfos)
      {
         try
         {
        	 String imgPath = i.getWordpressURL();
        	 String ext = null;
        	 Matcher m = PATTERN_IMG_EXTENSION.matcher(imgPath);
        	 if (m.find())
        	 {
        		 ext = m.group(0).replace(".", "");
        		 System.out.println(">>> Reading in image from: " + imgPath);
        		 BufferedImage img = ImageIO.read(new URL(imgPath));
        		 BufferedImage resized = Scalr.resize(img, Scalr.Mode.FIT_TO_WIDTH, 640);
        		 String paddedIndex = String.format("%03d", index++);
        		 File outputfile = new File("img/resized_" + paddedIndex + "." + ext);
        		 System.out.println(">>> Saving file to: " + outputfile.getAbsolutePath());
        		 ImageIO.write(resized, ext, outputfile);
        		 i.setResizedImageFile(outputfile);
        	 }
        	 else
        	 {
        		 System.err.println(">>> Could not find a valid image extension on the path: " + imgPath);
        		 System.exit(1);
        	 }
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }
   }

   private void uploadToImgur(Set<ImageInfo> imageInfos)
   {
      for (ImageInfo i : imageInfos)
      {
   	   uploadToImgur(i);
      }
   }

   private void uploadToImgur(ImageInfo imageInfo)
   {
      List<NameValuePair> postContent = new ArrayList<NameValuePair>(2);
      postContent.add(new BasicNameValuePair("image", imageInfo.getResizedImageFile().getAbsolutePath()));
      postContent.add(new BasicNameValuePair("type", "URL"));
   
      //String url = "http://api.imgur.com/2/upload";
      String url = "https://api.imgur.com/3/upload";
      HttpClient httpClient = new DefaultHttpClient();
      HttpContext localContext = new BasicHttpContext();
      HttpPost httpPost = new HttpPost(url);
   
      try
      {
         MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
         entity.addPart("type", new StringBody("file"));
         entity.addPart("image", new FileBody(imageInfo.getResizedImageFile()));
         
         httpPost.setHeader("Authorization", "Client-ID "+ API_KEY);
         httpPost.setEntity(entity);
   
         HttpResponse response = httpClient.execute(httpPost, localContext);
         JSONObject jsonObject = parseResponse(response);
         JSONObject data = (JSONObject) jsonObject.get("data");
         String link = (String) data.get("link");
         imageInfo.setImgurURL(link);
      }
      catch (IOException e)
      {
         e.printStackTrace();
         System.exit(1);
      }
      catch (JSONException e)
      {
         e.printStackTrace();
         System.exit(1);
      }
   }

   private JSONObject parseResponse(HttpResponse response) throws JSONException
   {
      String jsonResponse = null;
   
      try
      {
         jsonResponse = EntityUtils.toString(response.getEntity());
      }
      catch (ParseException e)
      {
         e.printStackTrace();
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
   
      if (jsonResponse == null)
         return null;
   
      return new JSONObject(jsonResponse);
   }

   private void stripOutParagraphTags(StringBuilder sb)
   {
      Pattern p = Pattern.compile("<p.*(imageInfo\\d*).*/p>");
      Matcher m = p.matcher(sb);
      int idx = 0;
      while (m.find(idx))
      {
         String s = m.group(1);
         sb.replace(m.start(), m.end(), s);
         idx = m.start() + 1;
      }
   }

   private String getBBPost(StringBuilder sb, Set<ImageInfo> imageInfos) 
   {
      stripOutParagraphTags(sb);
	   String bbPost = new String(sb.toString());
	   for (ImageInfo i : imageInfos) 
	   {
	      bbPost = bbPost.replaceAll(i.getTempReplacement(), "[IMG]" + i.getImgurURL() + "[/IMG]");
	   }
	   
	   return bbPost;
   }

   private static StringBuilder readFileIntoString(String filePath)
   {
      StringBuilder contents = new StringBuilder();
      File f = new File(filePath);
      try
      {
         String sCurrentLine;
         FileReader fr = new FileReader(f);
         BufferedReader br = new BufferedReader(fr);
         while ((sCurrentLine = br.readLine()) != null) 
         {
            contents.append(sCurrentLine);
            contents.append(NL);
         }
      }
      catch (IOException e)
      {
         System.err.println(">>>> File at path: [" + filePath + "] does not exist, please ensure the correct path.");
         System.exit(1);
      }
      
      return contents;
   }

   private void printBeans(Set<ImageInfo> imageInfos) 
   {
      for (ImageInfo i : imageInfos)
      {
   	   System.out.println(">>> ==================================");
   	   System.out.println(">>> i.getWordpressImgTag() = " + i.getWordpressImgTag());
   	   System.out.println(">>> i.getWordpressURL() = " + i.getWordpressURL());
   	   System.out.println(">>> i.getBBImgTag() = " + i.getBBImgTag());
   	   System.out.println(">>> i.getImgurURL() = " + i.getImgurURL());
      }
   }
}

 /*
 * Copyright 2004-2013 Pilz Ireland Industrial Automation Ltd. All Rights
 * Reserved. PILZ PROPRIETARY/CONFIDENTIAL.
 *
 * Created on 21 Aug 2013
 */