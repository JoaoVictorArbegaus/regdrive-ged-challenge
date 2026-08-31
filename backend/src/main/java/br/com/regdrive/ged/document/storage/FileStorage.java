package br.com.regdrive.ged.document.storage;

public interface FileStorage {

	String store(byte[] content);

	byte[] load(String fileKey);

	void delete(String fileKey);
}
