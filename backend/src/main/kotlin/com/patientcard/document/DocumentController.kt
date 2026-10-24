package com.patientcard.document

import com.patientcard.common.PageResponse
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
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/app/documents")
class DocumentController(private val documentService: DocumentService) {

    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PageableDefault(size = 20, sort = ["uploadedAt"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): PageResponse<DocumentResponse> = documentService.list(principal, pageable)

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable id: UUID): DocumentResponse =
        documentService.get(principal, id)

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun upload(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestPart("file") file: MultipartFile,
        @RequestParam documentType: String,
        @RequestParam title: String,
        @RequestParam(required = false) description: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) documentDate: LocalDate?,
    ): DocumentResponse = documentService.upload(principal, file, documentType, title, description, documentDate)

    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: DocumentUpdateRequest,
    ): DocumentResponse = documentService.update(principal, id, request)

    @GetMapping("/{id}/file")
    fun download(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
    ): ResponseEntity<FileSystemResource> {
        val file = documentService.download(principal, id)
        val disposition = ContentDisposition.attachment()
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
        documentService.delete(principal, id)
    }
}
