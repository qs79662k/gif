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

	private String signature = "";	//gif�ļ���ʶ���汾
	private String version = "";
	private int width;	//gif��ͼȫ�ֿ���
	private int height;	
	private boolean isColorTable;	//�Ƿ����ȫ����ɫ��
	@SuppressWarnings("unused")
	private int colorResolution;	//��ɫ�ֱ���
	private int[] colorTable;	//ȫ����ɫ��
	@SuppressWarnings("unused")
	private int backgroundColorIndex;	//ȫ�ֱ���ɫ����
	@SuppressWarnings("unused")
	private int pixelAspectRation;	//ͼ�������ؿ�߱�
	
	private String applicationInformation = "";	//����gif�ļ���Ӧ�ó��������Ϣ
	private int loopCount;	//ѭ������
	
	private byte[] commentData;	//���ߺ������κη�ͼ�����ݺͿ�����Ϣ���ı���Ϣ
	
	@SuppressWarnings("unused")
	private int textGridPositonX;	//�ı�x ��y
	@SuppressWarnings("unused")
	private int textGridPositonY;
	@SuppressWarnings("unused")
	private int textGridWidth;	//�ı�����
	@SuppressWarnings("unused")
	private int textGridHeight;
	@SuppressWarnings("unused")
	private int charCellWidth;	//�ַ���Ԫ�����
	@SuppressWarnings("unused")
	private int charCellHeight;
	@SuppressWarnings("unused")
	private int textForegroundColorIndex;	//�ı�ǰ��ɫ������ɫ����
	@SuppressWarnings("unused")
	private int textBackgroundColorIndex;
	@SuppressWarnings("unused")
	private byte[] plainTextData;	//��ʾ��Plain Text��Ϣ
	
	private int displayMethod;	//��ʾ�ڽ�����֡��Ⱦʱ��ǰһ֡���µ�ͼ�����δ���0�������κδ��� 1������ǰһ֡ͼ���ڴ˻����Ͻ�����Ⱦ 2����ԭΪ����ͼ�� 3����ԭΪ��һ��
	@SuppressWarnings("unused")
	private boolean isUserInputFlag;	//��ʾ�Ƿ���Ҫ�ڵõ��û�������ʱ�Ž�����һ֡�����루�����û�����ָʲô��Ӧ�ö�������false ��ʾ�����û�����,true��ʾ��Ҫ�û����롣
	private boolean isTransparentFlag;	//Transparent Flag������ֵΪtrueʱ������� Transparent Color Index ָ������ɫ��������͸��ɫ����Ϊfalse��������
	private int transparentColorIndex;	//͸������
	private int delayTime;	//����һ֡���ӳ�ʱ��
	
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
		frames.clear();	//���֡
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
		//��ȫ�ֿ���<=0ʱ��ʹ�õ�һ֡������Ϊȫ�ֿ���
		if(width <= 0) width = frames.get(0).width;
		if(height <= 0) height = frames.get(0).height;
		//��Ⱦ֡
		for(int i = 0 ; i < frames.size() ; i++) { 
			GifFrame frame = frames.get(i);	//��ǰ֡
			frame.image = new BufferedImage(width , height , BufferedImage.TYPE_4BYTE_ABGR);
			Graphics graphics = frame.image.getGraphics();
			//��Ⱦ����ɫ
			if(colorTable != null && backgroundColorIndex > -1 && backgroundColorIndex < colorTable.length) {
				int color = colorTable[backgroundColorIndex];
				int r = ConverTo256Color.getR(color);
				int g = ConverTo256Color.getG(color);
				int b = ConverTo256Color.getB(color); 
				graphics.setColor(new Color(r , g , b));
				graphics.fillRect(0 , 0, width, height);
			}
			//��Ⱦ����
			if(i > 0) {
				GifFrame _frame = frames.get(i - 1);	//ǰһ֡
				if(_frame.displayMethod != 2) {
					graphics.drawImage(_frame.image , 0 , 0 , null);
				} else {
					graphics.setColor(new Color(255 , 255 , 255));
					graphics.fillRect(_frame.x , _frame.y , _frame.width , _frame.height);
				} 
			}
			//�ͷŻ�����Դ
			graphics.dispose();
			//��Ⱦǰ��
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
		//�汾
		for(int i = 0 ; i < 3 ; i++) {
			version += (char)read();
		}
		//��ȡȫ�ֿ���
		width = readDoubleBytes();
		height = readDoubleBytes();
		//ѹ����װ�ֶ�
		int packedField = read();
		//1����ȫ����ɫ��0������
		isColorTable = (packedField & 0x80) != 0;
		//ɫ�ʷֱ���
		colorResolution = 1 << (((packedField & 0x70) >> 4) + 1);
		//Sort Flag����������ֵ 0 �� 1�����Ϊ 0 �� Global Color Table ����������Ϊ 1 ���ʾ Global Color Table ���ս������У�����Ƶ��������ɫ������ǰ��
//		boolean isSort = (disposalMethod & 0xf) != 0;
		//ȫ����ɫ���С
		int colorTableSize = 1 << ((packedField & 0x7) + 1);
		//��ȡ����ɫ����
		backgroundColorIndex = read();
		//ͼ���߱�
		pixelAspectRation = read();
		//��ȡȫ����ɫ��
		if(isColorTable) {	
			colorTable = readColorTable(colorTableSize);
		}	
	}
	
	private void readBody() {
		//���ʶ
		int flag = -1;
		while(flag != 0x3b) {		//0x3b GIF �������
			flag = read();				
			//��ȡ������
			switch(flag) {
				case 0x21 : 		//��չ��
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
		//Application Data�Ĵ�С���̶�λΪ11
		read();
		//Ӧ�ó�����Ϣ����������GIF�ļ���Ӧ�ó���������Ϣ
		for(int i = 0 ; i < 11 ; i++) {
			applicationInformation += (char)read();
		}
		//���С
//		read();
		//blockId
//		read();
		//ѭ������ռ�����ֽ�
//		loopCount = readDoubleBytes();			
		//���ս���
//		read();	
		//��ȡ������
		byte[] blockData = readBlockData();
		//ѭ������ռ�����ֽ�
		if(applicationInformation.equals("NETSCAPE2.0")) {
			loopCount = blockData[1] | blockData[2] << 8;
		}
	}
	
	private void readCommentExtension() {
		//Comment Data����˵��ͼ�Σ����ߺ������κη�ͼ�����ݺͿ�����Ϣ���ı���Ϣ
		commentData = readBlockData();
	}
	
	private void readPlainTextExtension() {
		//Block Size �̶�ֵ12
		read();
		//x��y
		textGridPositonX = readDoubleBytes();
		textGridPositonY = readDoubleBytes();
		//����
		textGridWidth = readDoubleBytes();
		textGridHeight = readDoubleBytes();
		//�ַ���Ԫ�����
		charCellWidth = read();
		charCellHeight = read();
		//�ı�ǰ��ɫ����
		textForegroundColorIndex = read();
		//�ı�����ɫ����
		textBackgroundColorIndex = read();
		//��ʾ���ַ������ж�������
		plainTextData = readBlockData();
	}
	
	private void readGraphicsControlExtension() {
		//Block Size��ʾ����������Ч�����ֽ���
		read();
		//ѹ����װ�ֶ�
		int packedField = read();
		//�����һ��������λReserved for Future User����λ�������ô�
		//Display Method����ʾ�ڽ�����֡��Ⱦʱ��ǰһ֡���µ�ͼ�����δ���0�������κδ��� 1������ǰһ֡ͼ���ڴ˻����Ͻ�����Ⱦ 2����ԭΪ����ͼ�� 3����ԭΪ��һ��
		displayMethod = (packedField & 0x1c) >> 2;
		//�������ڶ�λ��ʾ User Input Flag����ʾ�Ƿ���Ҫ�ڵõ��û�������ʱ�Ž�����һ֡�����루�����û�����ָʲô��Ӧ�ö�����0 ����ʾ�����û�����  1����ʾ��Ҫ�û�����
		isUserInputFlag = ((packedField & 0x2) >> 1) != 0;
		//���ұ�һλ����ʾ Transparent Flag������ֵΪ 1 ʱ������� Transparent Color Index ָ������ɫ��������͸��ɫ����Ϊ 0 ��������
		isTransparentFlag = (packedField & 0x1) != 0;
		//��ʾ GIF ��ͼÿһ֮֡��ļ������λΪ�ٷ�֮һ�룬���Դ�ֵ��*10����Ϊ 0 ʱ����ɽ���������
		delayTime = readDoubleBytes() * 10;
		//͸��ɫ����
		transparentColorIndex = read();
		//���ս���
		read();
	}
	
	private void readImageDescriptor() {
		GifFrame frame = new GifFrame();
		frames.add(frame);
		//��ȡ֡x��y����ƫ����
		frame.x = readDoubleBytes();
		frame.y = readDoubleBytes();
		//��ȡ֡����
		frame.width = readDoubleBytes();
		frame.height = readDoubleBytes();
		//ѹ����װ�ֶ�
		int packedField = read();
		//��������һλ��Local Color Table Flag����ʾ��һ֡ͼ���Ƿ���Ҫһ����������ɫ��1 Ϊ��Ҫ��0 Ϊ����Ҫ
		boolean isColorTable = (packedField & 0x80) != 0;
		//�������ڶ�λ��Interlace Flag����ʾ�Ƿ���Ҫ����ɨ�裬1 Ϊ��Ҫ��0 Ϊ����Ҫ
//		frame.isInterlace = (packedField & 0x40) != 0;
		//����������λ��Sort Flag�������Ҫ Local Color Table �Ļ�������ֶα�ʾ������˳��ͬ Global Color Table
//		frame.isSort = (packedField & 0x20) != 0;
		//���������ġ���λ��Reserved For Future Use������λ
		//�����������λ��Size of Local Color Table��ͬ Global Color Table �еĸ�λ������Ҫ�ֲ���ɫ��������Ч
		int colorTableSize = 1 << ((packedField & 0x7) + 1);
		//���ھֲ���ɫ��ʹ�þֲ���ɫ������ʹ��ȫ����ɫ��
		if(isColorTable) {
			frame.colorTable = readColorTable(colorTableSize);
		} else {
			frame.colorTable = colorTable;
		}
		//����displayMethod
		frame.displayMethod = displayMethod;
		//displayMethod��ʼΪ0
		displayMethod = 0;
		//����͸��ɫ
		if(isTransparentFlag) {
			frame.transparentColorIndex = transparentColorIndex;
		}
		//isTransparentFlag��ʼΪfalse
		isTransparentFlag = false;
		//����delayTime
		frame.delayTime = delayTime;	
		//delayTime��ʼΪ0
		delayTime = 0;
	
		//LZW��С����λ��
		int codeMiniSize = read();		
		//����ʼ��С
		int initCodeTableSize = 1 << codeMiniSize;
		//��ǰѹ������λ��
		int currentCodeSize = codeMiniSize + 1;
		//��ʼ���ֵ�
		String[] dictionary = initDictionary(initCodeTableSize);
		//���code
		int clearCode = initCodeTableSize;
		//����code
		int endOfInformation = clearCode + 1;
		//��ǰ������code
		int maxCode = endOfInformation + 1;
		//��ȡ֡ͼ����
		byte[] frameData = readBlockData();		
		//LZW����
		StringBuilder imageColorIndex = new StringBuilder();
	 
		String previous = null; 	//ǰһ������
		String current = null;		//��ǰ����
	
		int index = 0;		 //frameData�±�
		int startBit  = 0; 	//��ǰcode��ʼbitλ��
		int endBit = 0; 	//��ǰcode����bitλ��
		int code = 0;		//��ǰcode
		int remain = 0;		//ʣ��λ��
		int rightShift = 0;	//����
		int tempCode = 0;	//��ʱcode
		int bitsSize = 0;	//��ǰcode��ռbitλ�ܴ�С
		int count = 0;	//���㵱ǰcode��ռ�ֽ�����
		for(int i = 0 ; i < frame.width * frame.height ; i++) {
			code = 0;
			remain = currentCodeSize ;	
			rightShift = 0 ;	
			tempCode = 0 ;		
			bitsSize = currentCodeSize + startBit ;	
			count = bitsSize / 8 + (bitsSize % 8 > 0 ? 1 : 0);
			//������ȡ��ǰѹ����
			for(int j = 0 ; j < count ; j++) {
				if((startBit + remain) >= 8) {
					endBit = 7;		//��Ϊ����Ǵ�0��ʼ�����Խ���λ���Ϊ7
					tempCode = cutBits(frameData[index] , startBit , endBit);		//��ȡ��ǰbyte��bits�õ���ʱcode
					remain = remain - (8 - startBit);		//ʣ��λ��
					startBit = 0;	//�´ζ�ȡ��ʼbitλ
					index++;
				} else {
					endBit = startBit + remain - 1;	//��Ϊ����Ǵ�0��ʼ���������1
					tempCode = cutBits(frameData[index] , startBit , endBit);
					startBit = startBit + remain;
				}
				//�ϲ�code
				code = code | (tempCode << rightShift);		//ʹ����λ�ϲ�code
				//�����´κϲ�code����λ
				rightShift = currentCodeSize - remain;
			}

			//�����ֵ�
			if(code == clearCode) {	
				previous = null;
				maxCode = endOfInformation + 1;
				currentCodeSize = codeMiniSize + 1;
				dictionary = initDictionary(initCodeTableSize);
				continue;
			}
		
			//��ǰ֡�������
			if(code == endOfInformation) {			
				break;
			}
 
			/***********����֡��ɫ��������*********/
			
			String seq = dictionary[code];
			//seqΪnull��ȡprevious + previous�ĵ�һ���ַ���ֵ��current
			if(seq == null) {			
				current = previous + previous.substring(0 , 1);
			} else {
				current = seq;
			}
			
			//д���ֵ�
			if(previous != null && maxCode < dictionary.length) {	//previousΪ��˵����ǰcodeΪ��ʼ���ֵ���ȡ�ĵ�һ����code������Ҫд���ֵ�
				seq = previous + current.substring(0 , 1);
				dictionary[maxCode++] = seq;		//�����ֵ�
			}
		
			previous = current;
		
			imageColorIndex.append(current);
			
			/***********����֡��ɫ��������*********/
	
			//λ��+1����ΪGIF�淶���λ��=12������λ��=12�󲻿�������λ��
			if(maxCode == 1 << currentCodeSize && currentCodeSize < 12) {				
				currentCodeSize++;
			}
		}
		
		//imageColorIndexת��Ϊ֡��ɫ��������(charǿת��int�ȿ�)
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
		int[] colorTable = new int[256];
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