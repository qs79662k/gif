package com.wsp.gif;

import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MyGifDecoder {

	private String signature = "";	//gif文件标识、版本
	private String version = "";
	private int width;	//gif动图全局宽、高
	private int height;	
	private boolean isColorTable;	//是否存在全局颜色表
	@SuppressWarnings("unused")
	private int colorResolution;	//彩色分辨率
	private int[] colorTable;	//全局颜色表
	@SuppressWarnings("unused")
	private int backgroundColorIndex;	//全局背景色索引
	@SuppressWarnings("unused")
	private int pixelAspectRation;	//图像中像素宽高比
	
	private String applicationInformation = "";	//制作gif文件的应用程序相关信息
	private int loopCount;	//循环次数
	
	private byte[] commentData;	//作者和其他任何非图形数据和控制信息的文本信息
	
	@SuppressWarnings("unused")
	private int textGridPositonX;	//文本x 、y
	@SuppressWarnings("unused")
	private int textGridPositonY;
	@SuppressWarnings("unused")
	private int textGridWidth;	//文本宽、高
	@SuppressWarnings("unused")
	private int textGridHeight;
	@SuppressWarnings("unused")
	private int charCellWidth;	//字符单元格宽、高
	@SuppressWarnings("unused")
	private int charCellHeight;
	@SuppressWarnings("unused")
	private int textForegroundColorIndex;	//文本前景色、背景色索引
	@SuppressWarnings("unused")
	private int textBackgroundColorIndex;
	@SuppressWarnings("unused")
	private byte[] plainTextData;	//显示的Plain Text信息
	
	private int displayMethod;	//表示在进行逐帧渲染时，前一帧留下的图像作何处理，0：不做任何处理 1：保留前一帧图像，在此基础上进行渲染 2：还原为背景图像 3：还原为上一个
	@SuppressWarnings("unused")
	private boolean isUserInputFlag;	//表示是否需要在得到用户的输入时才进行下一帧的输入（具体用户输入指什么视应用而定）。false 表示无需用户输入,true表示需要用户输入。
	private boolean isTransparentFlag;	//Transparent Flag，当该值为true时，后面的 Transparent Color Index 指定的颜色将被当做透明色处理，为false则不做处理
	private int transparentColorIndex;	//透明索引
	private int delayTime;	//与下一帧的延迟时间
	
	private InputStream is;
	private List<GifFrame> frames = new ArrayList<GifFrame>();
	
	
	public String getSignature() {
		return signature;
	}
	
	public String getVersion() {
		return version;
	}
	
	public int getFrameCount() {
		return frames.size();
	}
	
	public int getDelayTime(int index) {
		return frames.get(index).delayTime;
	}
	
	public int getDisplayMethod(int index) {
		return frames.get(index).displayMethod;
	}
	
	public BufferedImage getFrame(int index) {
		return frames.get(index).image;
	}    
	
	public int getLoopCount() {
		return loopCount;
	}
	
	public byte[] getCommentData() {
		return commentData;
	}
	
	public void read(String file) {
		BufferedInputStream is = null;
		try {
			is = new BufferedInputStream(new FileInputStream(file));
			read(is);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void read(InputStream is) {
		this.is = is;
		frames.clear();	//清空帧
		readHeader();
		readBody();
		renderFrames();
		try {
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void renderFrames() {		
		//当全局宽、高<=0时，使用第一帧宽、高做为全局宽、高
		if(width <= 0) width = frames.get(0).width;
		if(height <= 0) height = frames.get(0).height;
		//渲染帧
		for(int i = 0 ; i < frames.size() ; i++) { 
			GifFrame frame = frames.get(i);	//当前帧
			frame.image = new BufferedImage(width , height , BufferedImage.TYPE_4BYTE_ABGR);
			Graphics graphics = frame.image.getGraphics();
			//渲染背景色
			if(colorTable != null && backgroundColorIndex > -1 && backgroundColorIndex < colorTable.length) {
				int color = colorTable[backgroundColorIndex];
				int r = ConverTo256Color.getR(color);
				int g = ConverTo256Color.getG(color);
				int b = ConverTo256Color.getB(color); 
				graphics.setColor(new Color(r , g , b));
				graphics.fillRect(0 , 0, width, height);
			}
			//渲染背景
			if(i > 0) {
				GifFrame _frame = frames.get(i - 1);	//前一帧
				if(_frame.displayMethod != 2) {
					graphics.drawImage(_frame.image , 0 , 0 , null);
				} else {
					graphics.setColor(new Color(255 , 255 , 255));
					graphics.fillRect(_frame.x , _frame.y , _frame.width , _frame.height);
				} 
			}
			//释放画笔资源
			graphics.dispose();
			//渲染前景
			for(int y = 0 ; y < frame.height ; y++) {
				for(int x = 0 ; x < frame.width ; x++) {
					if(frame.imageColorIndex.length > 0) {
						int index = (int)frame.imageColorIndex[y * frame.width + x];
						int color = frame.colorTable[index];
						if(frame.x + x < frame.image.getWidth() && frame.y + y < frame.image.getHeight()) {
							if(frame.transparentColorIndex == -1 || color != frame.colorTable[frame.transparentColorIndex]) {
								frame.image.setRGB(frame.x + x , frame.y + y , color);
							}
						} 
					}
				}
			}
		}
	}
	
	private void readHeader() {
		//GIF
		for(int  i = 0 ; i < 3 ; i++) {
			signature += (char)read();
		}
		//版本
		for(int i = 0 ; i < 3 ; i++) {
			version += (char)read();
		}
		//读取全局宽、高
		width = readDoubleBytes();
		height = readDoubleBytes();
		//压缩包装字段
		int packedField = read();
		//1存在全局颜色表，0不存在
		isColorTable = (packedField & 0x80) != 0;
		//色彩分辨率
		colorResolution = 1 << (((packedField & 0x70) >> 4) + 1);
		//Sort Flag，它有两个值 0 或 1，如果为 0 则 Global Color Table 不进行排序，为 1 则表示 Global Color Table 按照降序排列，出现频率最多的颜色排在最前面
//		boolean isSort = (disposalMethod & 0xf) != 0;
		//全局颜色表大小
		int colorTableSize = 1 << ((packedField & 0x7) + 1);
		//读取背景色索引
		backgroundColorIndex = read();
		//图像宽高比
		pixelAspectRation = read();
		//读取全局颜色表
		if(isColorTable) {	
			colorTable = readColorTable(colorTableSize);
		}	
	}
	
	private void readBody() {
		//块标识
		int flag = -1;
		while(flag != 0x3b) {		//0x3b GIF 解码结束
			flag = read();				
			//读取块数据
			switch(flag) {
				case 0x21 : 		//扩展块
					flag = read();				
					switch(flag) {
						case 0xff : 		//Application Extension
							readApplicationExtension();
							break;
						case 0xfe :		//Comment Extension
							readCommentExtension();
							break;
						case 0xf9 :		//Graphic Control Extension					
							readGraphicsControlExtension();
							break;
						case 0x01 : 	//Plain Text Extension
							readPlainTextExtension();
							break;
					}
					break;
				case 0x2c : 		//Image Descriptor
					readImageDescriptor();
					break;
			}		
		}
	}
	
	private void readApplicationExtension() {
		//Application Data的大小，固定位为11
		read();
		//应用程序信息，包含制作GIF文件的应用程序的相关信息
		for(int i = 0 ; i < 11 ; i++) {
			applicationInformation += (char)read();
		}
		//块大小
//		read();
		//blockId
//		read();
		//循环次数占两个字节
//		loopCount = readDoubleBytes();			
		//块终结器
//		read();	
		//读取块数据
		byte[] blockData = readBlockData();
		//循环次数占两个字节
		if(applicationInformation.equals("NETSCAPE2.0")) {
			loopCount = blockData[1] | blockData[2] << 8;
		}
	}
	
	private void readCommentExtension() {
		//Comment Data用来说明图形，作者和其他任何非图形数据和控制信息的文本信息
		commentData = readBlockData();
	}
	
	private void readPlainTextExtension() {
		//Block Size 固定值12
		read();
		//x、y
		textGridPositonX = readDoubleBytes();
		textGridPositonY = readDoubleBytes();
		//宽、高
		textGridWidth = readDoubleBytes();
		textGridHeight = readDoubleBytes();
		//字符单元格宽、高
		charCellWidth = read();
		charCellHeight = read();
		//文本前景色索引
		textForegroundColorIndex = read();
		//文本背景色索引
		textBackgroundColorIndex = read();
		//显示的字符串，有多个块组成
		plainTextData = readBlockData();
	}
	
	private void readGraphicsControlExtension() {
		//Block Size表示接下来的有效数据字节数
		read();
		//压缩包装字段
		int packedField = read();
		//从左边一、二、三位Reserved for Future User保留位，暂无用处
		//Display Method，表示在进行逐帧渲染时，前一帧留下的图像作何处理，0：不做任何处理 1：保留前一帧图像，在此基础上进行渲染 2：还原为背景图像 3：还原为上一个
		displayMethod = (packedField & 0x1c) >> 2;
		//从右数第二位表示 User Input Flag，表示是否需要在得到用户的输入时才进行下一帧的输入（具体用户输入指什么视应用而定）0 ：表示无需用户输入  1：表示需要用户输入
		isUserInputFlag = ((packedField & 0x2) >> 1) != 0;
		//最右边一位，表示 Transparent Flag，当该值为 1 时，后面的 Transparent Color Index 指定的颜色将被当做透明色处理，为 0 则不做处理
		isTransparentFlag = (packedField & 0x1) != 0;
		//表示 GIF 动图每一帧之间的间隔，单位为百分之一秒，所以此值需*10，当为 0 时间隔由解码器管理
		delayTime = readDoubleBytes() * 10;
		//透明色索引
		transparentColorIndex = read();
		//块终结器
		read();
	}
	
	private void readImageDescriptor() {
		GifFrame frame = new GifFrame();
		frames.add(frame);
		//读取帧x、y方向偏移量
		frame.x = readDoubleBytes();
		frame.y = readDoubleBytes();
		//读取帧宽、高
		frame.width = readDoubleBytes();
		frame.height = readDoubleBytes();
		//压缩包装字段
		int packedField = read();
		//从左数第一位：Local Color Table Flag，表示下一帧图像是否需要一个独立的颜色表，1 为需要，0 为不需要
		boolean isColorTable = (packedField & 0x80) != 0;
		//从左数第二位：Interlace Flag，表示是否需要隔行扫描，1 为需要，0 为不需要
//		frame.isInterlace = (packedField & 0x40) != 0;
		//从左数第三位：Sort Flag，如果需要 Local Color Table 的话，这个字段表示其排列顺序，同 Global Color Table
//		frame.isSort = (packedField & 0x20) != 0;
		//从左数第四、五位：Reserved For Future Use，保留位
		//从左数最后三位：Size of Local Color Table，同 Global Color Table 中的该位，如需要局部颜色表，则其有效
		int colorTableSize = 1 << ((packedField & 0x7) + 1);
		//存在局部颜色表使用局部颜色表，否则使用全局颜色表
		if(isColorTable) {
			frame.colorTable = readColorTable(colorTableSize);
		} else {
			frame.colorTable = colorTable;
		}
		//设置displayMethod
		frame.displayMethod = displayMethod;
		//displayMethod初始为0
		displayMethod = 0;
		//设置透明色
		if(isTransparentFlag) {
			frame.transparentColorIndex = transparentColorIndex;
		}
		//isTransparentFlag初始为false
		isTransparentFlag = false;
		//设置delayTime
		frame.delayTime = delayTime;	
		//delayTime初始为0
		delayTime = 0;
	
		//LZW最小编码位长
		int codeMiniSize = read();		
		//码表初始大小
		int initCodeTableSize = 1 << codeMiniSize;
		//当前压缩编码位长
		int currentCodeSize = codeMiniSize + 1;
		//初始化字典
		String[] dictionary = initDictionary(initCodeTableSize);
		//清除code
		int clearCode = initCodeTableSize;
		//结束code
		int endOfInformation = clearCode + 1;
		//当前码表最大code
		int maxCode = endOfInformation + 1;
		//读取LZW编码帧图数据
		byte[] frameData = readBlockData();		
		//帧图颜色索引
		StringBuilder imageColorIndex = new StringBuilder();
	 
		String previous = null; 	//前一个串码
		String current = null;		//当前串码
	
		int index = 0;		 //frameData下标
		int startBit  = 0; 	//当前code开始bit位置
		int endBit = 0; 	//当前code结束bit位置
		int code = 0;		//当前code
		int remain = 0;		//剩余位长
		int rightShift = 0;	//右移
		int tempCode = 0;	//临时code
		int bitsSize = 0;	//当前code所占bit位总大小
		int count = 0;	//计算当前code所占字节数量
		for(int i = 0 ; i < frame.width * frame.height ; i++) {
			code = 0;
			remain = currentCodeSize ;	
			rightShift = 0 ;	
			tempCode = 0 ;		
			bitsSize = currentCodeSize + startBit ;	
			count = bitsSize / 8 + (bitsSize % 8 > 0 ? 1 : 0);
			//遍历读取当前压缩码
			for(int j = 0 ; j < count ; j++) {
				if((startBit + remain) >= 8) {
					endBit = 7;		//因为起点是从0开始，所以结束位最大为7
					tempCode = cutBits(frameData[index] , startBit , endBit);		//截取当前byte的bits得到临时code
					remain = remain - (8 - startBit);		//剩余位长
					startBit = 0;	//下次读取开始bit位
					index++;
				} else {
					endBit = startBit + remain - 1;	//因为起点是从0开始，所以需减1
					tempCode = cutBits(frameData[index] , startBit , endBit);
					startBit = startBit + remain;
				}
				//合并code
				code = code | (tempCode << rightShift);		//使用移位合并code
				//计算下次合并code右移位
				rightShift = currentCodeSize - remain;
			}

			//重置字典
			if(code == clearCode) {	
				previous = null;
				maxCode = endOfInformation + 1;
				currentCodeSize = codeMiniSize + 1;
				dictionary = initDictionary(initCodeTableSize);
				continue;
			}
		
			//当前帧解码结束
			if(code == endOfInformation) {			
				break;
			}
 
			/***********解码帧颜色索引数据*********/
			
			String seq = dictionary[code];
			//seq为null，取previous + previous的第一个字符赋值给current
			if(seq == null) {			
				current = previous + previous.substring(0 , 1);
			} else {
				current = seq;
			}
			
			//写入字典
			if(previous != null && maxCode < dictionary.length) {	//previous为空说明当前code为初始化字典后读取的第一个根code并不需要写入字典
				seq = previous + current.substring(0 , 1);
				dictionary[maxCode++] = seq;		//加入字典
			}
		
			previous = current;
		
			imageColorIndex.append(current);
			
			/***********解码帧颜色索引数据*********/
	
			//位长+1，因为GIF规范最大位长=12，所以位长=12后不可再增加位长
			if(maxCode == 1 << currentCodeSize && currentCodeSize < 12) {				
				currentCodeSize++;
			}
		}
		
		//imageColorIndex转换为帧颜色索引数组(char强转成int既可)
		frame.imageColorIndex = imageColorIndex.toString().toCharArray();		
	}
	
	private String[] initDictionary(int initCodeTableSize){	
		String[] dictionary = new String[4096];
		for(int i = 0 ; i  < initCodeTableSize ; i++) {	
			dictionary[i] = String.valueOf((char)i);
		}		
		return dictionary;
	}
	
	private int read(){
		try {
			return is.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	private int readDoubleBytes() {
		return read() | read() << 8;
	}
	
	private int[] readColorTable(int colorTableSize) {
		int[] colorTable = new int[colorTableSize];
		for(int i = 0 ; i < colorTableSize ; i++) {
			int r = read();
			int g = read();
			int b = read();
			colorTable[i] = converRgbToColor(r , g , b);
		}
		return colorTable;
	}

	private byte[] readBlockData() {
		int blockSize = 0;
		byte[] blockData = new byte[0];
		while((blockSize = read()) > 0) {
			byte[] tempData = blockData;
			blockData = new byte[tempData.length + blockSize];
			System.arraycopy(tempData , 0 , blockData , 0 , tempData.length);
			try {
				is.read(blockData , tempData.length , blockSize);
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}			
		return blockData;
	}
	
	private int cutBits(byte code , int start , int end) {		
		return (0xff >> (7 - (end - start))) & ((((int)code) & 0xff) >> start);
	}
	
	private int converRgbToColor(int r , int g , int b) {
		return ((0xff << 24) | (r << 16) | (g << 8) | b);
	}
	
}
