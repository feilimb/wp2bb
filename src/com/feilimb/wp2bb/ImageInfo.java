package com.feilimb.wp2bb;

import java.io.File;

public class ImageInfo
{
	public String getWordpressImgTag() 
	{
		return _wordpressImgTag;
	}
	
	public void setWordpressImgTag(String _wordpressImgTag) 
	{
		this._wordpressImgTag = _wordpressImgTag;
	}
	
	public String getBBImgTag() 
	{
		return _bbImgTag;
	}
	
	public void setBBImgTag(String _bbImgTag) 
	{
		this._bbImgTag = _bbImgTag;
	}
	
	public String getWordpressURL() 
	{
		return _wordpressURL;
	}
	
	public void setWordpressURL(String _wordpressURL) 
	{
		this._wordpressURL = _wordpressURL;
	}
	
	public String getImgurURL() 
	{
		return _imgurURL;
	}
	
	public void setImgurURL(String _imgurURL) 
	{
		this._imgurURL = _imgurURL;
	}
	
	public void setResizedImageFile(File resizedImage)
	{
		this._resizedImage = resizedImage;
	}
	
	public File getResizedImageFile()
	{
		return this._resizedImage;
	}
	
	public void setTempReplacement(String temp)
	{
	   this._tempReplacement = temp;
	}
	
	public String getTempReplacement()
	{
	   return this._tempReplacement;
	}
	
	private String _wordpressImgTag;
	private String _bbImgTag;
	private String _wordpressURL;
	private String _imgurURL;
	private File _resizedImage;
	private String _tempReplacement;
}
