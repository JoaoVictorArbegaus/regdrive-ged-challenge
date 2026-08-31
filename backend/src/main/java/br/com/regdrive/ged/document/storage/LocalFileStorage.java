package br.com.regdrive.ged.document.storage;

import br.com.regdrive.ged.document.exception.FileStorageException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalFileStorage implements FileStorage {

	private final Path rootDirectory;

	public LocalFileStorage(@Value("${app.storage.path}") String storagePath) {
		this.rootDirectory = Path.of(storagePath).toAbsolutePath().normalize();
	}

	@Override
	public String store(byte[] content) {
		String fileKey = UUID.randomUUID().toString();
		Path destination = resolve(fileKey);
		try {
			Files.createDirectories(rootDirectory);
			Files.write(destination, content, StandardOpenOption.CREATE_NEW);
			return fileKey;
		} catch (IOException exception) {
			throw new FileStorageException("Não foi possível armazenar o arquivo.", exception);
		}
	}

	@Override
	public byte[] load(String fileKey) {
		try {
			return Files.readAllBytes(resolve(fileKey));
		} catch (IOException exception) {
			throw new FileStorageException("Não foi possível carregar o arquivo.", exception);
		}
	}

	@Override
	public void delete(String fileKey) {
		try {
			Files.deleteIfExists(resolve(fileKey));
		} catch (IOException exception) {
			throw new FileStorageException("Não foi possível remover o arquivo.", exception);
		}
	}

	private Path resolve(String fileKey) {
		Path filePath = rootDirectory.resolve(fileKey).normalize();
		if (!filePath.getParent().equals(rootDirectory)) {
			throw new FileStorageException("Chave de arquivo inválida.");
		}
		return filePath;
	}
}
