package me.thosea.developersdungeon.util;

import org.jetbrains.annotations.Nullable;

import java.awt.Color;

public class AverageColorCounter {
	private int r = 0;
	private int g = 0;
	private int b = 0;
	private int factors = 0;

	public void addColor(@Nullable Color color) {
		if(color == null) return;

		r += color.getRed();
		g += color.getGreen();
		b += color.getBlue();
		factors++;
	}

	public Color average() {
		if(factors == 0) {
			return new Color(255, 255, 255);
		} else {
			return new Color(r / factors, g / factors, b / factors);
		}
	}
}