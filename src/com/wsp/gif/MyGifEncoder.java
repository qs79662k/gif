package com.wsp.gif;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MyGifEncoder {
	
	public final static int NOT_DISPOSE = 0;	//不做任何处理
	public final static int RESERVED_FRAME = 1;	//保留前一帧在此基础上渲染
	public final static int REVERT_TO_BACKGROUND = 2;	//还原为背景图像
	public final static int REVERT_TO_PREVIOUS = 3;		//还原为上一个
	
	private int width;	//全局宽、高
	private int height;
	private int[] colorTable;	//全局颜色表
	private int backgroundColorIndex = -1;	//全局背景色索引	
	private Color backgroundColor;		//全局背景色
	private int loopCount;	//循环次数，当为0时无限循环播放，默认值为0
	private boolean startFlag = true;	//开始编码标识
	private OutputStream os;

	
	/**
	 * 设置全局宽
	 * @param width
	 */
	public void setWidth(int width) {
		this.width = width;
	}
	
	/**
	 * 设置全局高
	 * @param height
	 */
	public void setHeight(int height) {
		this.height = height;
	}
	
	/**
	 * 指定第一帧的一个颜色做为背景色
	 * @param backgroundColor 
	 */
	public void setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
	}
	
	/**
	 * 设置动画循环播放次数，0为无限播放
	 * @param loopCount
	 */
	public void setLoopCount(int loopCount) {
		this.loopCount = loopCount;
	}
	
	public void start(String file) {
		BufferedOutputStream os = null;
		try {
			os = new BufferedOutputStream(new FileOutputStream(file));
			start(os);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void start(OutputStream os) {
		init();
		this.os = os;
	}
	
	public void finish() {
		write(0x3b);
		try {
			os.flush();
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 初始化全局数据
	 */
	private void init() {
		width = 0;
		height = 0;
		colorTable = null;
		backgroundColorIndex = -1;
		backgroundColor = null;
		loopCount = 0;
		startFlag = true;
		os = null;
	}

	public MyGifEncoder addFrame(BufferedImage image) {
		return addFrame(image , null , 0 , 0 , 0 , NOT_DISPOSE);
	}
	
	public MyGifEncoder addFrame(BufferedImage image , int delayTime) {
		return addFrame(image , null , 0 , 0 , delayTime , NOT_DISPOSE);
	}
	
	public MyGifEncoder addFrame(BufferedImage image , int delayTime , int displayMethod) {
		return addFrame(image , null , 0 , 0 , delayTime , displayMethod);
	}	
	
	public MyGifEncoder addFrame(BufferedImage image , Color transparentColor , int delayTime , int displayMethod) {
		return addFrame(image , transparentColor , 0 , 0  , delayTime , displayMethod);
	}
	
	/**
	 * 添加帧
	 * @param image	帧图
	 * @param transparentColor	指定帧中的一个颜色做为透明色
	 * @param x	帧x偏移
	 * @param y	帧y偏移
	 * @param delayTime	与下一帧的延迟时间(MS)
	 * @param displayMethod 表示在进行逐帧渲染时，前一帧留下的图像作何处理，0：不做任何处理 1：保留前一帧图像，在此基础上进行渲染 2：还原为背景图像 3：还原为上一个
	 * @return
	 */
	public MyGifEncoder addFrame(BufferedImage image , Color transparentColor , int x , int y , int delayTime , int displayMethod) {
		GifFrame frame = new GifFrame();
		frame.image = ConverTo256Color.create(image);
		frame.x = x;
		frame.y = y;
		frame.width = image.getWidth();
		frame.height = image.getHeight();
		frame.delayTime = delayTime / 10;
		frame.displayMethod = displayMethod;
		frame.colorTable = getColorTable(image);
		frame.transparentColor = transparentColor;
		//当startFlag==true说明当前帧为第一帧
		if(startFlag) {
			//全局width<=0时使用第一帧width
			if(width <= 0) {
				width = frame.width;
			}
			//全局height<=0时使用第一帧height
			if(height <= 0) {
				height = frame.height;
			}
			//全局颜色表
			colorTable = frame.colorTable;
			//当全局颜色表包含当前帧所有颜色时，使用全局颜色表
			frame.colorTable = null;
			//写出块数据
			writeHeader();
			writeApplicationExtension();
			startFlag = false;
		} else if(isFullInclusionColor(colorTable , frame.colorTable)) {
			//当全局颜色表包含当前帧所有颜色时，使用全局颜色表
			frame.colorTable = null;
		}
		//写出块数据
		writeGraphicsControlExtension(frame);
		writeImageDescriptor(frame);
		return this;
	}
	
	private void writeHeader() {
		//写入gif文件标识
		write("GIF".getBytes());
		//写入gif版本
		write("89a".getBytes());
		//写入全局宽
		writeDoubleBytes(width);
		//写入全局高
		writeDoubleBytes(height);
		//压缩包装字段
		int packedField = 0;
		//存在全局颜色表，第1位为1否则为0
		packedField = packedField | (colorTable == null ? 0 : 1) << 7;
		//彩色分辨率
		packedField = packedField | (colorTable == null ? 0 : (Integer.toBinaryString(colorTable.length - 1).length() - 1)) << 4;
		//Sort Flag，它有两个值 0 或 1，如果为 0 则 Global Color Table 不进行排序，为 1 则表示 Global Color Table 按照降序排列，出现频率最多的颜色排在最前面
		packedField = packedField | 0 << 3;
		//全局颜色表大小
		packedField = packedField | (colorTable == null ? 0 : (Integer.toBinaryString(colorTable.length - 1).length() - 1));
		//写入压缩包装字段
		write(packedField);
		//写入背景色索引
		if(backgroundColor != null) {
			backgroundColorIndex = findResembleColorIndex(colorTable, backgroundColor);
		}
		write(backgroundColorIndex);
		//写入像素纵横比
		write(0);
		//写入全局颜色表，依次写入r\g\b值
		if(colorTable != null) {
			writeColorTable(colorTable);
		}
	}
	
	private void writeApplicationExtension() {
		//flag0x21
		write(0x21);
		//flag0xff
		write(0xff);
		//Application Data的大小，固定位为11
		write(11);	
		//应用程序信息，包含制作GIF文件的应用程序的相关信息，固定为11个字符
		write("NETSCAPE2.0".getBytes());
		//blockSize
		write(3);
		//blockId
		write(1);
		//写入循环次数，0无限循环
		writeDoubleBytes(loopCount);
		//块终结
		write(0x00);		
	}
	
	private void writeGraphicsControlExtension(GifFrame frame) {
		//flag0x21
		write(0x21);
		//flag0xf9
		write(0xf9);
		//blockSize
		write(4);
		//包装字段
		int packedField = 0;
		//从左边一、二、三位Reserved for Future User保留位，暂无用处
		//Display Method，表示在进行逐帧渲染时，前一帧留下的图像作何处理，0：不做任何处理 1：保留前一帧图像，在此基础上进行渲染 2：还原为背景图像 3：还原为上一个
		packedField = packedField | frame.displayMethod << 2;
		//从右数第二位表示 User Input Flag，表示是否需要在得到用户的输入时才进行下一帧的输入（具体用户输入指什么视应用而定）0 ：表示无需用户输入  1：表示需要用户输入
		//最右边一位，表示 Transparent Flag，当该值为 1 时，后面的 Transparent Color Index 指定的颜色将被当做透明色处理，为 0 则不做处理
		packedField = packedField | (frame.transparentColor == null ? 0 : 1);	
		write(packedField);
		//与下一帧的间隔时间
		writeDoubleBytes(frame.delayTime);
		//透明色索引
		if(frame.transparentColor != null) {
			int[] colorTable = frame.colorTable == null ? this.colorTable : frame.colorTable;
			frame.transparentColorIndex = findResembleColorIndex(colorTable, frame.transparentColor);
		}
		write(frame.transparentColorIndex);
		//块终结器
		write(0x00);
	}
	
	private void writeImageDescriptor(GifFrame frame) {	
		//块标识0x2c
		write(0x2c);
		
		//写入x、y偏移量
		writeDoubleBytes(frame.x);
		writeDoubleBytes(frame.y);
		//写入独立宽、高
		writeDoubleBytes(frame.width);
		writeDoubleBytes(frame.height);
	
		//压缩包装字段
		int packedField = 0;
		//是否存在独立颜色表
		boolean isColorTable = frame.colorTable != null;
		//左起第一位是否存在独立颜色表
		if(isColorTable) {
			packedField = packedField | 1 << 7;
		}
		//颜色表排列顺序默认无，所以无需设置
		//从左数第四、五位：Reserved For Future Use，保留位，无需设置
		//颜色表大小3位
		packedField = packedField | (isColorTable == true ? Integer.toBinaryString(frame.colorTable.length - 1).length() - 1 : 0);
		//写入处置方法
		write(packedField);	

		//写入颜色表
		if(isColorTable) {
			writeColorTable(frame.colorTable);
		} else {	//当帧颜色表为空时，使用全局颜色表
			frame.colorTable = colorTable;
		}

		//LZW最小编码位长
		int codeMiniSize = Integer.toBinaryString(frame.colorTable.length - 1).length();
		write(codeMiniSize);

		//码表初始大小
		int initCodeTableSize = 1 << codeMiniSize;
		//当前压缩编码位长
		int currentCodeSize = codeMiniSize + 1;
		//清除code
		int clearCode = initCodeTableSize;
		//结束code
		int endOfInformation = clearCode + 1;
		//当前码表最大code
		int maxCode = endOfInformation + 1;
		//字典
		Map<String , Integer> dictionary = initDictionary(initCodeTableSize);

		//帧图颜色索引值
		int[] frameColorIndex = new int[frame.image.getWidth() * frame.image.getHeight()];
		for(int y = 0 ; y < frame.image.getHeight(); y++) {
			for(int x = 0 ; x < frame.image.getWidth(); x++) {
				int color = frame.image.getRGB(x , y);			
				for(int i = 0 ; i < frame.colorTable.length ; i++) {
					if(color == frame.colorTable[i]) {
						frameColorIndex[y * frame.image.getWidth() + x] = i;								
					}
				}
			}
		}

		String current = "";	//当前编码
		String next = "";		//下一个编码	
		//帧LZW编码数据
		int lzwDataSize = 0;
		byte[] lzwData = new byte[frameColorIndex.length];	
		for(int i = 0 ; i < frameColorIndex.length ; i++) {			
			//当前编码串
			current = String.valueOf((char)frameColorIndex[i]);		
			while(i + 1 < frameColorIndex.length && dictionary.get(current + String.valueOf((char)frameColorIndex[i + 1])) != null) {
				i++;
				current += String.valueOf((char)frameColorIndex[i]);
			}
			//下一个编码
			if(i + 1 < frameColorIndex.length) {
				next = String.valueOf((char)frameColorIndex[i + 1]);
			}
			//编码写入字典
			String seq = current + next;
			dictionary.put(seq , maxCode++);

			int code = dictionary.get(current);
			lzwDataSize = addLzwData(code , currentCodeSize , lzwData , lzwDataSize , false);
			
			if(i == frameColorIndex.length - 1) {			
				//结束code
				code = endOfInformation;
				lzwDataSize = addLzwData(code , currentCodeSize , lzwData , lzwDataSize , true);
			}
		
			if(maxCode == 4096) {
				//重置code
				code = clearCode;
				lzwDataSize = addLzwData(code , currentCodeSize , lzwData , lzwDataSize , false);
				//重置字典
				currentCodeSize = codeMiniSize + 1;		
				maxCode = endOfInformation + 1;
				dictionary = initDictionary(initCodeTableSize);
			}
			
			//增加当前位长
			if(maxCode - 1 == 1 << currentCodeSize && currentCodeSize < 12) {					
				currentCodeSize++;
			}
		}
		
		writeBlockData(Arrays.copyOfRange(lzwData , 0 , lzwDataSize));
		
		//块终结器
		write(0x00);
	}
	
	private void write(int n) {
		try {
			os.write((byte)n);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void write(byte[] bs) {
		try {
			os.write(bs);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 整型数据以两个字节写入
	 * @param n
	 */
	private void writeDoubleBytes(int n) {
		write(n & 0xff);
		write(n >> 8);
	}
	
	/**
	 * 写入一个块数据
	 * 写入规则为：分割blockData为n个minBlockData，依次写入minBlockDataSize(被分割的块大小)、minBlockData(被分割块数据)...minBlockDataSize、minBlockData
	 * @param blockData
	 */
	private void writeBlockData(byte[] blockData) {
		int start = 0;
		int end = start + (blockData.length - start >= 255 ? 255 : blockData.length - start);
		int count = blockData.length  / 255 + (blockData.length % 255 == 0 ? 0 : 1);
		byte[] minBlockData = null;
		for(int i = 0 ; i < count ; i++) {
			minBlockData = new byte[end - start];
			for(int j = start ; j < end ; j++) {
				minBlockData[j - start] = blockData[j];
			}					
			start = end;
			end = start + (blockData.length - start >= 255 ? 255 : blockData.length - start);
			write(minBlockData.length);
			write(minBlockData);
		}
	}
	
	/**
	 * 写入颜色表
	 * 写入规则：依次写入r\g\b...r\g\b
	 * @param colorTable
	 */
	private void writeColorTable(int[] colorTable) {
		int colorTableSize = 1 << Integer.toBinaryString(colorTable.length - 1).length();
		byte[] rgbs = new byte[colorTableSize * 3];
		for(int i = 0 ; i < colorTable.length ; i++) {
			if(i < colorTable.length) {
				int color = colorTable[i];
				rgbs[i * 3] =  (byte)ConverTo256Color.getR(color);
				rgbs[i * 3 + 1] = (byte)ConverTo256Color.getG(color);
				rgbs[i * 3 + 2] = (byte)ConverTo256Color.getB(color);	
			}
		}
		write(rgbs);
	}
	
	private int data = 0;
	private int dataStart = 0;	
	/**
	 * 添加lzw编码到lzwData数组
	 * @param code lzw编码
	 * @param currentCodeSize 当前lzw编码位长
	 * @param lzwData lzw编码数组
	 * @param lzwDataSize lzw已编码个数
	 * @param isEnd 当前编码是否为结束码
	 * @return lzw已编码个数
	 */
	private int addLzwData(int code , int currentCodeSize , byte[] lzwData , int lzwDataSize , boolean isEnd) { 
		int codeStart  = 0; 	//当前code开始bit位置
		int codeRemain = currentCodeSize;		//剩余位长
		int codeEnd = codeStart + (codeRemain < 8 - dataStart ? codeRemain : 8 - dataStart); 	//当前code结束bit位置
		int codeBitSize = currentCodeSize + dataStart;	//当前code所占bit位总大小
		int count = codeBitSize / 8 + (codeBitSize % 8 > 0 ? 1 : 0);	//计算当前code所需字节数量
		int tempCode = 0;
		for(int i = 0 ; i < count ; i++) {
			tempCode = cutBits(code , codeStart , codeEnd);
			data = data | tempCode << dataStart;
			if(codeRemain >= 8 - dataStart) {				
				codeStart = codeStart + (8 - dataStart); 
				codeRemain = codeRemain - (8 - dataStart);				
				lzwData[lzwDataSize++] = (byte)data;	
				data = 0;
				dataStart = 0;
			} else {
				dataStart = dataStart + codeRemain;	
			}				
			codeEnd = codeStart + (codeRemain < 8 - dataStart ? codeRemain : 8 - dataStart);
			if(isEnd && i == count - 1) {
				lzwData[lzwDataSize++] = (byte)data;
				data = 0;
				dataStart = 0;
			}
		}
		return lzwDataSize;
	}
	
	/**
	 * primaryColorTable颜色表是否包含secondaryColorTable颜色表所有颜色 
	 * 包含返回true，不包含返回false
	 * @param primaryColorTable
	 * @param secondaryColorTable
	 * @return
	 */
	private boolean isFullInclusionColor(int[] primaryColorTable , int[] secondaryColorTable) {
		Map<Integer , Integer> colorTableMap = getColorTableMap(primaryColorTable);
		for(int i = 0 ; i < secondaryColorTable.length ; i++) {
			int color = secondaryColorTable[i];
			if(colorTableMap.get(color) == null) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 颜色表转换为Map
	 * @param colorTable
	 * @return
	 */
	private Map<Integer , Integer> getColorTableMap(int[] colorTable) {
		Map<Integer , Integer> colorTableMap = new HashMap<>();
		for(int i = 0 ; i < colorTable.length ; i++) {
			int color = colorTable[i];
			colorTableMap.put(color , color);
		}
		return colorTableMap;
	}
	
	/**
	 * 从帧图中获取颜色表
	 * @param image
	 * @return
	 */
	private int[] getColorTable(BufferedImage image) {
		Map<Integer , Integer> map = new HashMap<Integer , Integer>();
		for(int y = 0 ; y < image.getHeight() ; y++) {
			for(int x = 0 ; x < image.getWidth() ; x++) {
				int color = image.getRGB(x, y);
				map.put(color, color);
			}
		}	
		int i = 0;
		int colorTableSize = Integer.max(3 , map.size());
		int[] colorTable = new int[colorTableSize];
		for(Integer color : map.keySet()) {
			colorTable[i++] = color;
		}		
		return colorTable;
	}
	
	/**
	 * 在颜色表中找最相似的颜色索引
	 * @param colorTable
	 * @param color
	 * @return
	 */
	private int findResembleColorIndex(int[] colorTable , Color color) {
		int r1 = color.getRed();
		int g1 = color.getGreen();
		int b1 = color.getBlue();
		int resembleColorIndex = 0;
		double e = 0;
		for(int i = 0 ; i < colorTable.length ; i++) {
			int rgb = colorTable[i];
			int r2 = ConverTo256Color.getR(rgb);
			int g2 = ConverTo256Color.getG(rgb);
			int b2 = ConverTo256Color.getB(rgb);
			int r = Math.abs(r1 - r2);
			int g = Math.abs(g1 - g2);
			int b = Math.abs(b1 - b2);
			double _e = Math.sqrt(Math.pow(r, 2) + Math.pow(g, 2) + Math.pow(b, 2));
			if(i == 0) {
				e = _e;
				resembleColorIndex = i;
			} else if(e > _e) {
				e = _e;
				resembleColorIndex = i;
			}
		}
		return resembleColorIndex;
	}
	
	/**
	 * 初始化LZW字典
	 * @param initCodeTableSize	初始LZW字典大小
	 * @return
	 */
	private Map<String , Integer> initDictionary(int initCodeTableSize){
		Map<String , Integer> dictionary = new HashMap<String , Integer>();
		for(int i = 0 ; i < initCodeTableSize ; i++) {
			dictionary.put(String.valueOf((char)i), i);
		}	
		dictionary.put("重置字典--占位", initCodeTableSize);
		dictionary.put("编码结束--占位", initCodeTableSize + 1);	
		return dictionary;
	}
	
	/**
	 * 按位截取并位移到最右边
	 * @param code 被截取数据的编码
	 * @param start	截取位开始位置
	 * @param end 截取位结束位置
	 * @return
	 */
	private int cutBits(int code , int start , int end) {		
		return (0x7fffffff >> (0x1f - (end - start))) & (code >> start);	
	}
	
}
