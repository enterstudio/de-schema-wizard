package com.deleidos.dp.beans;

import java.math.BigInteger;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author leegc
 * @author yoonj1
 *
 */
public class BinaryDetail extends Detail {
	private Histogram byteHistogram;
	private String mimeType;
	private BigInteger length;
	private String hash;
	private double entropy;

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public BigInteger getLength() {
		return length;
	}

	public void setLength(BigInteger length) {
		this.length = length;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	@Override
	@JsonProperty("byte-freq-histogram")
	public Histogram getHistogram() {
		return byteHistogram;
	}

	@JsonProperty("byte-freq-histogram")
	public void setByteHistogram(Histogram characterHistogram) {
		this.byteHistogram = characterHistogram;
	}

	public double getEntropy() {
		return entropy;
	}

	public void setEntropy(double entropy) {
		this.entropy = entropy;
	}

}
