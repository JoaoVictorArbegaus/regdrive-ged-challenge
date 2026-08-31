package br.com.regdrive.ged.document.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.regdrive.ged.document.exception.DocumentArchivedException;
import br.com.regdrive.ged.document.exception.InvalidDocumentStatusTransitionException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentTest {

	@Test
	void updateMetadataReplacesValuesAndClearsOptionalFields() {
		Document document = new Document(
				"Contrato", "Descrição", Set.of("antiga"), "tenant-demo", UUID.randomUUID());
		Instant previousUpdate = document.getUpdatedAt();

		document.updateMetadata(" Contrato atualizado ", "", Set.of());

		assertThat(document.getTitle()).isEqualTo("Contrato atualizado");
		assertThat(document.getDescription()).isNull();
		assertThat(document.getTags()).isEmpty();
		assertThat(document.getUpdatedAt()).isAfterOrEqualTo(previousUpdate);
	}

	@Test
	void publishedDocumentCanBeUpdatedButArchivedDocumentCannot() {
		Document document = new Document(
				"Contrato", null, Set.of(), "tenant-demo", UUID.randomUUID());
		document.transitionTo(DocumentStatus.PUBLISHED);

		document.updateMetadata("Publicado", "Descrição", Set.of("vigente"));

		assertThat(document.getTitle()).isEqualTo("Publicado");
		document.transitionTo(DocumentStatus.ARCHIVED);
		assertThatThrownBy(() -> document.updateMetadata("Outro", null, Set.of()))
				.isInstanceOf(DocumentArchivedException.class);
		assertThat(document.getTitle()).isEqualTo("Publicado");
	}

	@Test
	void validStatusTransitionsReachArchived() {
		Document document = new Document(
				"Contrato", null, Set.of(), "tenant-demo", UUID.randomUUID());

		document.transitionTo(DocumentStatus.PUBLISHED);
		assertThat(document.getStatus()).isEqualTo(DocumentStatus.PUBLISHED);
		document.transitionTo(DocumentStatus.ARCHIVED);
		assertThat(document.getStatus()).isEqualTo(DocumentStatus.ARCHIVED);
	}

	@Test
	void invalidRepeatedAndBackwardStatusTransitionsAreRejected() {
		Document draft = new Document("Rascunho", null, Set.of(), "tenant-demo", UUID.randomUUID());
		assertThatThrownBy(() -> draft.transitionTo(DocumentStatus.DRAFT))
				.isInstanceOf(InvalidDocumentStatusTransitionException.class);
		assertThatThrownBy(() -> draft.transitionTo(DocumentStatus.ARCHIVED))
				.isInstanceOf(InvalidDocumentStatusTransitionException.class);

		Document published = new Document("Publicado", null, Set.of(), "tenant-demo", UUID.randomUUID());
		published.transitionTo(DocumentStatus.PUBLISHED);
		assertThatThrownBy(() -> published.transitionTo(DocumentStatus.PUBLISHED))
				.isInstanceOf(InvalidDocumentStatusTransitionException.class);
		assertThatThrownBy(() -> published.transitionTo(DocumentStatus.DRAFT))
				.isInstanceOf(InvalidDocumentStatusTransitionException.class);

		published.transitionTo(DocumentStatus.ARCHIVED);
		assertThatThrownBy(() -> published.transitionTo(DocumentStatus.PUBLISHED))
				.isInstanceOf(InvalidDocumentStatusTransitionException.class);
	}
}
