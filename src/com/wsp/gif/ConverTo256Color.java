package com.wsp.gif;

import java.awt.image.BufferedImage;

public class ConverTo256Color {
	
	public static BufferedImage create(BufferedImage image) {
		return create(image , 256);
	}
	
	public static BufferedImage create(BufferedImage image , int colorSize) {
		//ͳ��ͼƬ��R/G/Bֵ���4λ������ɫ���ֵ�Ƶ��
		int[] colorsFrequency = new int[4096];
		for(int y = 0 ; y < image.getHeight() ; y++) {
			for(int x = 0 ; x < image.getWidth() ; x++) {
				int highColor = getHighColor(image.getRGB(x, y));
				colorsFrequency[highColor]++;
			}
		}
		
		//��ɫд����ɫ���У���ΪȡR\G\Bֵ�����4λ���õ���ֵΪ0-4095������i��Ϊ��ɫֵд��
		int[] colors = new int[4096];
		for(int i = 0 ; i < colors.length ; i++) {
			colors[i] = i;
		}
		
		//������ɫ��ͼƬ�г��ֵ�Ƶ�ʸߵ�������ɫ��
		for(int i = 0 ; i < colors.length ; i++) {
			for(int j = i+1 ; j < colors.length ; j++) {
				int frequency1 = colorsFrequency[colors[i]];
				int frequency2 = colorsFrequency[colors[j]];
				if(frequency1 < frequency2) {
					int temp = colors[i];
					colors[i] = colors[j];
					colors[j] = temp;
				}
			}
		}
	
		//����Ƶ��ֵ���ǳ���Ƶ����ߵ�ǰcolorSize��ֵ��Ϊ-1
		for(int i = 0 ; i < colors.length ; i++) {
			int color = colors[i];
			if(i < colorSize) {
				//��colorSizeɫ��ɫ����
				colorsFrequency[color] = i;
			} else {
				//����colorSizeɫ��ɫ����
				colorsFrequency[color] = -1;
			}
		}
		
		//����ͼƬ��ɫΪcolorSizeɫ
		for(int y = 0 ; y < image.getHeight() ; y++) {
			for(int x = 0 ; x < image.getWidth(); x++) {
				int highColor = getHighColor(image.getRGB(x, y));
				if(colorsFrequency[highColor] > -1) {
					image.setRGB(x , y, getRevertColor(highColor));
				} else {
					image.setRGB(x , y, getRevertColor(findResembleColor(colors , colorSize , highColor)));
				}
			}
		}
		
		return image;
	}
	
	public static int findResembleColor(int[] colors , int color) {
		return findResembleColor(colors , colors.length , color);
	}
	
	//�������Ƶ���ɫ
	public static int findResembleColor(int[] colors , int colorSize , int color) {
		int r1 = getR(color);
		int g1 = getG(color);
		int b1 = getB(color);
		int resembleColor = 0;
		double e = 0;
		for(int i = 0 ; i < colorSize ; i++) {
			int r2 = getR(colors[i]);
			int g2 = getG(colors[i]);
			int b2 = getB(colors[i]);
			int r = Math.abs(r1 - r2);
			int g = Math.abs(g1 - g2);
			int b = Math.abs(b1 - b2);
			double tempE = Math.sqrt(Math.pow(r, 2) + Math.pow(g, 2) + Math.pow(b, 2));
			if(i == 0) {
				e = tempE;
				resembleColor = colors[i];
			} else if(e > tempE) {
				e = tempE;
				resembleColor = colors[i];
			}
		}
		return resembleColor;
	}
	
	//��4λ��ɫ
	public static int getHighColor(int color) {
		int r = (color & 0x0F00000) >> 20;
        int g = (color & 0x000F000) >> 12;
        int b = (color & 0x00000F0) >> 4;
        return r << 8 | g << 4 | b;
	}
	
	//��ԭ��ɫ
	public static int getRevertColor(int color) {
        int r = (color & 0x0F00) << 12;
        int g = (color & 0x000F0) << 8;
        int b = (color & 0x00000F) << 4;
        return r | g | b;
    }
	
	public static int getR(int color) {
		return color >> 16 & 0xff;
	}
	
	public static int getG(int color) {
		return color >> 8 & 0xff;
	}
	
	public static int getB(int color) {
		return color & 0xff;
	}
	
}
