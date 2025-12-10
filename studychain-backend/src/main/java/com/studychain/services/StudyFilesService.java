package com.studychain.services;

import com.studychain.models.StudyFile;
import com.studychain.models.StudyNote;
import com.studychain.repositories.StudyFileRepository;
import com.studychain.repositories.StudyNoteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.io.UncheckedIOException;

@Service
public class StudyFilesService {

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".pdf");

	private final StudyFileRepository fileRepository;
	private final StudyNoteRepository noteRepository;
	private final Path uploadDir;

	public StudyFilesService(
		StudyFileRepository fileRepository,
		StudyNoteRepository noteRepository,
		@Value("${studyfiles.upload.dir:uploads/studyfiles}") String uploadDirPath
	) throws IOException {
		this.fileRepository = fileRepository;
		this.noteRepository = noteRepository;
		this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
		Files.createDirectories(this.uploadDir);
	}

	public List<StudyFile> listFiles(Long ownerUserId) {
		return fileRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(ownerUserId);
	}

	public List<StudyNote> listNotes(Long ownerUserId) {
		return noteRepository.findAllByOwnerUserIdOrderByUpdatedAtDesc(ownerUserId);
	}

	public List<StudyFile> listAllFiles() {
		return fileRepository.findAllByOrderByCreatedAtDesc();
	}

	public List<StudyNote> listAllNotes() {
		return noteRepository.findAllByOrderByUpdatedAtDesc();
	}

	public StudyFile getFile(Long id) {
		return fileRepository.findById(id).orElse(null);
	}

	public StudyNote getNote(Long id) {
		return noteRepository.findById(id).orElse(null);
	}

	public Resource getResource(StudyFile file) {
		return new FileSystemResource(file.getFilePath());
	}

	public MediaType resolveMediaType(StudyFile file) {
		try {
			return MediaType.parseMediaType(file.getContentType());
		} catch (Exception ignore) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}

	@Transactional
	public StudyFile saveUpload(MultipartFile file, Long ownerUserId) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File is empty");
		}
		String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload");
		String ext = "";
		int dot = originalFilename.lastIndexOf('.');
		if (dot > -1 && dot < originalFilename.length() - 1) {
			ext = originalFilename.substring(dot).toLowerCase();
		}
		if (!ALLOWED_EXTENSIONS.contains(ext)) {
			throw new IllegalArgumentException("Only images (jpg, jpeg, png, gif, webp) and pdf are allowed");
		}
		String storedName = UUID.randomUUID().toString() + ext;
		Path target = uploadDir.resolve(storedName);
		Files.copy(file.getInputStream(), target);

		StudyFile sf = new StudyFile();
		sf.setOwnerUserId(ownerUserId);
		sf.setOriginalName(originalFilename);
		sf.setFilePath(target.toString());
		sf.setContentType(file.getContentType() != null ? file.getContentType() : (".pdf".equals(ext) ? "application/pdf" : "image/" + ext.replace(".", "")));
		return fileRepository.save(sf);
	}

	@Transactional
	public StudyNote createNote(Long ownerUserId, String title, String content) {
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("Note content is required");
		}
		StudyNote note = new StudyNote();
		note.setOwnerUserId(ownerUserId);
		note.setTitle((title != null && !title.isBlank()) ? title : "Note");
		note.setContent(content);
		return noteRepository.save(note);
	}

	@Transactional
	public boolean deleteFile(StudyFile file) {
		try {
			if (file.getFilePath() != null && !file.getFilePath().isBlank()) {
				try {
					Files.deleteIfExists(Paths.get(file.getFilePath()));
				} catch (IOException io) {
					// ignore physical delete errors; still remove db row
				}
			}
			fileRepository.delete(file);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Transactional
	public boolean deleteNote(StudyNote note) {
		try {
			noteRepository.delete(note);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Transactional
	public StudyNote updateNote(StudyNote note, String title, String content) {
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("Note content is required");
		}
		note.setTitle((title != null && !title.isBlank()) ? title : note.getTitle());
		note.setContent(content);
		return noteRepository.save(note);
	}
}


