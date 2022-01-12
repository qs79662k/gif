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
	
	public final static int NOT_DISPOSE = 0;	//�����κδ���
	public final static int RESERVED_FRAME = 1;	//����ǰһ֡�ڴ˻�������Ⱦ
	public final static int REVERT_TO_BACKGROUND = 2;	//��ԭΪ����ͼ��
	public final static int REVERT_TO_PREVIOUS = 3;		//��ԭΪ��һ��
	
	private int width;	//ȫ�ֿ���
	private int height;
	private int[] colorTable;	//ȫ����ɫ��
	private int backgroundColorIndex = -1;	//ȫ�ֱ���ɫ����	
	private Color backgroundColor;		//ȫ�ֱ���ɫ
	private int loopCount;	//ѭ����������Ϊ0ʱ����ѭ�����ţ�Ĭ��ֵΪ0
	private boolean startFlag = true;	//��ʼ�����ʶ
	private OutputStream os;

	
	/**
	 * ����ȫ�ֿ�
	 * @param width
	 */
	public void setWidth(int width) {
		this.width = width;
	}
	
	/**
	 * ����ȫ�ָ�
	 * @param height
	 */
	public void setHeight(int height) {
		this.height = height;
	}
	
	/**
	 * ָ����һ֡��һ����ɫ��Ϊ����ɫ
	 * @param backgroundColor 
	 */
	public void setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
	}
	
	/**
	 * ���ö���ѭ�����Ŵ�����0Ϊ���޲���
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
	 * ��ʼ��ȫ������
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
	 * ���֡
	 * @param image	֡ͼ
	 * @param transparentColor	ָ��֡�е�һ����ɫ��Ϊ͸��ɫ
	 * @param x	֡xƫ��
	 * @param y	֡yƫ��
	 * @param delayTime	����һ֡���ӳ�ʱ��(MS)
	 * @param displayMethod ��ʾ�ڽ�����֡��Ⱦʱ��ǰһ֡���µ�ͼ�����δ���0�������κδ��� 1������ǰһ֡ͼ���ڴ˻����Ͻ�����Ⱦ 2����ԭΪ����ͼ�� 3����ԭΪ��һ��
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
		//��startFlag==true˵����ǰ֡Ϊ��һ֡
		if(startFlag) {
			//ȫ��width<=0ʱʹ�õ�һ֡width
			if(width <= 0) {
				width = frame.width;
			}
			//ȫ��height<=0ʱʹ�õ�һ֡height
			if(height <= 0) {
				height = frame.height;
			}
			//ȫ����ɫ��
			colorTable = frame.colorTable;
			//��ȫ����ɫ�������ǰ֡������ɫʱ��ʹ��ȫ����ɫ��
			frame.colorTable = null;
			//д��������
			writeHeader();
			writeApplicationExtension();
			startFlag = false;
		} else if(isFullInclusionColor(colorTable , frame.colorTable)) {
			//��ȫ����ɫ�������ǰ֡������ɫʱ��ʹ��ȫ����ɫ��
			frame.colorTable = null;
		}
		//д��������
		writeGraphicsControlExtension(frame);
		writeImageDescriptor(frame);
		return this;
	}
	
	private void writeHeader() {
		//д��gif�ļ���ʶ
		write("GIF".getBytes());
		//д��gif�汾
		write("89a".getBytes());
		//д��ȫ�ֿ�
		writeDoubleBytes(width);
		//д��ȫ�ָ�
		writeDoubleBytes(height);
		//ѹ����װ�ֶ�
		int packedField = 0;
		//����ȫ����ɫ����1λΪ1����Ϊ0
		packedField = packedField | (colorTable == null ? 0 : 1) << 7;
		//��ɫ�ֱ���
		packedField = packedField | (colorTable == null ? 0 : (Integer.toBinaryString(colorTable.length - 1).length() - 1)) << 4;
		//Sort Flag����������ֵ 0 �� 1�����Ϊ 0 �� Global Color Table ����������Ϊ 1 ���ʾ Global Color Table ���ս������У�����Ƶ��������ɫ������ǰ��
		packedField = packedField | 0 << 3;
		//ȫ����ɫ���С
		packedField = packedField | (colorTable == null ? 0 : (Integer.toBinaryString(colorTable.length - 1).length() - 1));
		//д��ѹ����װ�ֶ�
		write(packedField);
		//д�뱳��ɫ����
		if(backgroundColor != null) {
			backgroundColorIndex = findResembleColorIndex(colorTable, backgroundColor);
		}
		write(backgroundColorIndex);
		//д�������ݺ��
		write(0);
		//д��ȫ����ɫ������д��r\g\bֵ
		if(colorTable != null) {
			writeColorTable(colorTable);
		}
	}
	
	private void writeApplicationExtension() {
		//flag0x21
		write(0x21);
		//flag0xff
		write(0xff);
		//Application Data�Ĵ�С���̶�λΪ11
		write(11);	
		//Ӧ�ó�����Ϣ����������GIF�ļ���Ӧ�ó���������Ϣ���̶�Ϊ11���ַ�
		write("NETSCAPE2.0".getBytes());
		//blockSize
		write(3);
		//blockId
		write(1);
		//д��ѭ��������0����ѭ��
		writeDoubleBytes(loopCount);
		//���ս�
		write(0x00);		
	}
	
	private void writeGraphicsControlExtension(GifFrame frame) {
		//flag0x21
		write(0x21);
		//flag0xf9
		write(0xf9);
		//blockSize
		write(4);
		//��װ�ֶ�
		int packedField = 0;
		//�����һ��������λReserved for Future User����λ�������ô�
		//Display Method����ʾ�ڽ�����֡��Ⱦʱ��ǰһ֡���µ�ͼ�����δ���0�������κδ��� 1������ǰһ֡ͼ���ڴ˻����Ͻ�����Ⱦ 2����ԭΪ����ͼ�� 3����ԭΪ��һ��
		packedField = packedField | frame.displayMethod << 2;
		//�������ڶ�λ��ʾ User Input Flag����ʾ�Ƿ���Ҫ�ڵõ��û�������ʱ�Ž�����һ֡�����루�����û�����ָʲô��Ӧ�ö�����0 ����ʾ�����û�����  1����ʾ��Ҫ�û�����
		//���ұ�һλ����ʾ Transparent Flag������ֵΪ 1 ʱ������� Transparent Color Index ָ������ɫ��������͸��ɫ����Ϊ 0 ��������
		packedField = packedField | (frame.transparentColor == null ? 0 : 1);	
		write(packedField);
		//����һ֡�ļ��ʱ��
		writeDoubleBytes(frame.delayTime);
		//͸��ɫ����
		if(frame.transparentColor != null) {
			int[] colorTable = frame.colorTable == null ? this.colorTable : frame.colorTable;
			frame.transparentColorIndex = findResembleColorIndex(colorTable, frame.transparentColor);
		}
		write(frame.transparentColorIndex);
		//���ս���
		write(0x00);
	}
	
	private void writeImageDescriptor(GifFrame frame) {	
		//���ʶ0x2c
		write(0x2c);
		
		//д��x��yƫ����
		writeDoubleBytes(frame.x);
		writeDoubleBytes(frame.y);
		//д���������
		writeDoubleBytes(frame.width);
		writeDoubleBytes(frame.height);
	
		//ѹ����װ�ֶ�
		int packedField = 0;
		//�Ƿ���ڶ�����ɫ��
		boolean isColorTable = frame.colorTable != null;
		//�����һλ�Ƿ���ڶ�����ɫ��
		if(isColorTable) {
			packedField = packedField | 1 << 7;
		}
		//��ɫ������˳��Ĭ���ޣ�������������
		//���������ġ���λ��Reserved For Future Use������λ����������
		//��ɫ���С3λ
		packedField = packedField | (isColorTable == true ? Integer.toBinaryString(frame.colorTable.length - 1).length() - 1 : 0);
		//д�봦�÷���
		write(packedField);	

		//д����ɫ��
		if(isColorTable) {
			writeColorTable(frame.colorTable);
		} else {	//��֡��ɫ��Ϊ��ʱ��ʹ��ȫ����ɫ��
			frame.colorTable = colorTable;
		}

		//LZW��С����λ��
		int codeMiniSize = Integer.toBinaryString(frame.colorTable.length - 1).length();
		write(codeMiniSize);

		//����ʼ��С
		int initCodeTableSize = 1 << codeMiniSize;
		//��ǰѹ������λ��
		int currentCodeSize = codeMiniSize + 1;
		//���code
		int clearCode = initCodeTableSize;
		//����code
		int endOfInformation = clearCode + 1;
		//��ǰ������code
		int maxCode = endOfInformation + 1;
		//�ֵ�
		Map<String , Integer> dictionary = initDictionary(initCodeTableSize);

		//֡ͼ��ɫ����ֵ
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

		String current = "";	//��ǰ����
		String next = "";		//��һ������	
		//֡LZW��������
		int lzwDataSize = 0;
		byte[] lzwData = new byte[frameColorIndex.length];	
		for(int i = 0 ; i < frameColorIndex.length ; i++) {			
			//��ǰ���봮
			current = String.valueOf((char)frameColorIndex[i]);		
			while(i + 1 < frameColorIndex.length && dictionary.get(current + String.valueOf((char)frameColorIndex[i + 1])) != null) {
				i++;
				current += String.valueOf((char)frameColorIndex[i]);
			}
			//��һ������
			if(i + 1 < frameColorIndex.length) {
				next = String.valueOf((char)frameColorIndex[i + 1]);
			}
			//����д���ֵ�
			String seq = current + next;
			dictionary.put(seq , maxCode++);

			int code = dictionary.get(current);
			lzwDataSize = addLzwData(code , currentCodeSize , lzwData , lzwDataSize , false);
			
			if(i == frameColorIndex.length - 1) {			
				//����code
				code = endOfInformation;
				lzwDataSize = addLzwData(code , currentCodeSize , lzwData , lzwDataSize , true);
			}
		
			if(maxCode == 4096) {
				//����code
				code = clearCode;
				lzwDataSize = addLzwData(code , currentCodeSize , lzwData , lzwDataSize , false);
				//�����ֵ�
				currentCodeSize = codeMiniSize + 1;		
				maxCode = endOfInformation + 1;
				dictionary = initDictionary(initCodeTableSize);
			}
			
			//���ӵ�ǰλ��
			if(maxCode - 1 == 1 << currentCodeSize && currentCodeSize < 12) {					
				currentCodeSize++;
			}
		}
		
		writeBlockData(Arrays.copyOfRange(lzwData , 0 , lzwDataSize));
		
		//���ս���
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
	 * ���������������ֽ�д��
	 * @param n
	 */
	private void writeDoubleBytes(int n) {
		write(n & 0xff);
		write(n >> 8);
	}
	
	/**
	 * д��һ��������
	 * д�����Ϊ���ָ�blockDataΪn��minBlockData������д��minBlockDataSize(���ָ�Ŀ��С)��minBlockData(���ָ������)...minBlockDataSize��minBlockData
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
	 * д����ɫ��
	 * д���������д��r\g\b...r\g\b
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
	 * ���lzw���뵽lzwData����
	 * @param code lzw����
	 * @param currentCodeSize ��ǰlzw����λ��
	 * @param lzwData lzw��������
	 * @param lzwDataSize lzw�ѱ������
	 * @param isEnd ��ǰ�����Ƿ�Ϊ������
	 * @return lzw�ѱ������
	 */
	private int addLzwData(int code , int currentCodeSize , byte[] lzwData , int lzwDataSize , boolean isEnd) { 
		int codeStart  = 0; 	//��ǰcode��ʼbitλ��
		int codeRemain = currentCodeSize;		//ʣ��λ��
		int codeEnd = codeStart + (codeRemain < 8 - dataStart ? codeRemain : 8 - dataStart); 	//��ǰcode����bitλ��
		int codeBitSize = currentCodeSize + dataStart;	//��ǰcode��ռbitλ�ܴ�С
		int count = codeBitSize / 8 + (codeBitSize % 8 > 0 ? 1 : 0);	//���㵱ǰcode�����ֽ�����
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
	 * primaryColorTable��ɫ���Ƿ����secondaryColorTable��ɫ��������ɫ 
	 * ��������true������������false
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
	 * ��ɫ��ת��ΪMap
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
	 * ��֡ͼ�л�ȡ��ɫ��
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
	 * ����ɫ�����������Ƶ���ɫ����
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
	 * ��ʼ��LZW�ֵ�
	 * @param initCodeTableSize	��ʼLZW�ֵ��С
	 * @return
	 */
	private Map<String , Integer> initDictionary(int initCodeTableSize){
		Map<String , Integer> dictionary = new HashMap<String , Integer>();
		for(int i = 0 ; i < initCodeTableSize ; i++) {
			dictionary.put(String.valueOf((char)i), i);
		}	
		dictionary.put("�����ֵ�--ռλ", initCodeTableSize);
		dictionary.put("�������--ռλ", initCodeTableSize + 1);	
		return dictionary;
	}
	
	/**
	 * ��λ��ȡ��λ�Ƶ����ұ�
	 * @param code ����ȡ���ݵı���
	 * @param start	��ȡλ��ʼλ��
	 * @param end ��ȡλ����λ��
	 * @return
	 */
	private int cutBits(int code , int start , int end) {		
		return (0x7fffffff >> (0x1f - (end - start))) & (code >> start);	
	}
	
}
