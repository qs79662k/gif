package com.wsp.gif;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class GifFrame {
	
	protected int x;	//֡ƫ��
	protected int y;
	protected int width;	//֡����
	protected int height;
	protected int[] colorTable;	//֡��ɫ��
	protected int displayMethod;	//��ʾ�ڽ�����֡��Ⱦʱ��ǰһ֡���µ�ͼ�����δ���0�������κδ��� 1������ǰһ֡ͼ���ڴ˻����Ͻ�����Ⱦ 2����ԭΪ����ͼ�� 3����ԭΪ��һ��
	protected int delayTime;	//��һ֡�ӳ�ʱ��
	protected char[] imageColorIndex;	//֡ͼ����
	protected int transparentColorIndex = -1;	//֡͸��ɫ����
	protected Color transparentColor;
	protected BufferedImage image;

}
