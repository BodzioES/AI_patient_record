package com.patientcard.image

import com.patientcard.common.PageResponse
import com.patientcard.entity.ImageCategory
import com.patientcard.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.core.io.FileSystemResource
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.CacheControl
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/app/images")
class ImageController(private val imageService: ImageService) {

    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) symptomId: UUID?,
        @PageableDefault(size = 20, sort = ["imageNumber"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): PageResponse<ImageResponse> = imageService.list(principal, symptomId, pageable)

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable id: UUID): ImageResponse =
        imageService.get(principal, id)

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun upload(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestPart("file") file: MultipartFile,
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) description: String?,
        @RequestParam(required = false) category: ImageCategory?,
        @RequestParam(required = false) symptomId: UUID?,
        @RequestParam(required = false) documentId: UUID?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) capturedAt: Instant?,
        @RequestParam(required = false) sequenceNumber: Int?,
    ): ImageResponse = imageService.upload(
        principal = principal,
        file = file,
        title = title,
        description = description,
        category = category ?: ImageCategory.OTHER,
        symptomId = symptomId,
        documentId = documentId,
        capturedAt = capturedAt,
        sequenceNumber = sequenceNumber,
    )

    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ImageUpdateRequest,
    ): ImageResponse = imageService.update(principal, id, request)

    @GetMapping("/{id}/file")
    fun download(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
    ): ResponseEntity<FileSystemResource> {
        val file = imageService.download(principal, id)
        val disposition = ContentDisposition.inline()
            .filename(file.originalFilename, StandardCharsets.UTF_8)
            .build()
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.mimeType))
            .contentLength(file.sizeBytes)
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(FileSystemResource(file.path))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable id: UUID) {
        imageService.delete(principal, id)
    }
}
