package br.com.regdrive.ged.document.storage;

public interface FileStorage {

	String store(byte[] content);

	void delete(String fileKey);
}
