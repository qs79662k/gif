package com.wsp.gif;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class GifFrame {
	
	protected int x;	//帧偏移
	protected int y;
	protected int width;	//帧宽、高
	protected int height;
	protected int[] colorTable;	//帧颜色表
	protected int displayMethod;	//表示在进行逐帧渲染时，前一帧留下的图像作何处理，0：不做任何处理 1：保留前一帧图像，在此基础上进行渲染 2：还原为背景图像 3：还原为上一个
	protected int delayTime;	//下一帧延迟时间
	protected char[] imageColorIndex;	//帧图数组
	protected int transparentColorIndex = -1;	//帧透明色索引
	protected Color transparentColor;
	protected BufferedImage image;

}
