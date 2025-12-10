package com.studychain.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "study_files", indexes = {
	@Index(name = "idx_study_files_user_created", columnList = "ownerUserId,createdAt")
})
public class StudyFile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long ownerUserId;

	@Column(nullable = false, length = 255)
	private String originalName;

	@Column(nullable = false, length = 500)
	private String filePath;

	@Column(nullable = false, length = 100)
	private String contentType;

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	public Long getId() {
		return id;
	}

	public Long getOwnerUserId() {
		return ownerUserId;
	}

	public void setOwnerUserId(Long ownerUserId) {
		this.ownerUserId = ownerUserId;
	}

	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}


