package br.com.regdrive.ged.document.dto;

public record DocumentVersionDownload(
		String filename,
		String mimeType,
		long fileSize,
		byte[] content) {
}
