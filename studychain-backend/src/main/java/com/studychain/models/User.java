package com.studychain.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "uk_users_username", columnList = "username", unique = true),
        @Index(name = "uk_users_email", columnList = "email", unique = true)
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank
    @Size(min = 1, max = 80)
    @Column(nullable = false, length = 80)
    private String firstName;

    @NotBlank
    @Size(min = 1, max = 80)
    @Column(nullable = false, length = 80)
    private String lastName;

    @Past
    private LocalDate birthDate;

    @NotBlank
    @Size(min = 4, max = 255)
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

	@Column(name = "profile_image", columnDefinition = "bytea")
	private byte[] profileImage;

	@Column(name = "profile_image_content_type", length = 100)
	private String profileImageContentType;

	@Column(name = "theme_preference", length = 20)
	private String themePreference;

	@Column(name = "cover_image", columnDefinition = "bytea")
	private byte[] coverImage;

	@Column(name = "cover_image_content_type", length = 100)
	private String coverImageContentType;

	@Column(name = "display_name_mode", length = 20)
	private String displayNameMode; // "username" or "full_name"

	@Column(name = "show_birth_date")
	private Boolean showBirthDatePublic = true;

	@Column(name = "show_joined_date")
	private Boolean showJoinedDatePublic = true;

	@Column(name = "about", length = 2000)
	private String about;
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

	public byte[] getProfileImage() {
		return profileImage;
	}

	public void setProfileImage(byte[] profileImage) {
		this.profileImage = profileImage;
	}

	public String getProfileImageContentType() {
		return profileImageContentType;
	}

	public void setProfileImageContentType(String profileImageContentType) {
		this.profileImageContentType = profileImageContentType;
	}

	public String getThemePreference() {
		return themePreference;
	}

	public void setThemePreference(String themePreference) {
		this.themePreference = themePreference;
	}

	public byte[] getCoverImage() {
		return coverImage;
	}

	public void setCoverImage(byte[] coverImage) {
		this.coverImage = coverImage;
	}

	public String getCoverImageContentType() {
		return coverImageContentType;
	}

	public void setCoverImageContentType(String coverImageContentType) {
		this.coverImageContentType = coverImageContentType;
	}

	public String getDisplayNameMode() {
		return displayNameMode;
	}

	public void setDisplayNameMode(String displayNameMode) {
		this.displayNameMode = displayNameMode;
	}

	public Boolean getShowBirthDatePublic() {
		return showBirthDatePublic;
	}

	public void setShowBirthDatePublic(Boolean showBirthDatePublic) {
		this.showBirthDatePublic = showBirthDatePublic;
	}

	public Boolean getShowJoinedDatePublic() {
		return showJoinedDatePublic;
	}

	public void setShowJoinedDatePublic(Boolean showJoinedDatePublic) {
		this.showJoinedDatePublic = showJoinedDatePublic;
	}

	public String getAbout() {
		return about;
	}

	public void setAbout(String about) {
		this.about = about;
	}
}


