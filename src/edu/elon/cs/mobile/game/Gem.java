package edu.elon.cs.mobile.game;

import java.util.Random;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

public class Gem {

	private int color;
	private RectF rect;
	private int row, col;

	public Gem(int color, int row, int col) {
		this.color = color;
		this.row = row;
		this.col = col;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;

	}

	public RectF getRect() {
		return rect;
	}

	public void setRect(RectF rect) {
		this.rect = rect;
	}

	public int getRow() {
		return row;
	}

	public void setRow(int row) {
		this.row = row;
	}

	public int getCol() {
		return col;
	}

	public void setCol(int col) {
		this.col = col;
	}

	@Override
	public String toString() {
		return "Gem [color=" + color + ", row=" + row + ", col=" + col + "]";
	}
	
	
	
}
