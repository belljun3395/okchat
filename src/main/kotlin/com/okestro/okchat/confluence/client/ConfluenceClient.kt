package com.okestro.okchat.confluence.client

import feign.Param
import feign.RequestLine

interface ConfluenceClient {

    /**
     * Get space by key
     *
     * @param spaceKey The space key
     * @return Space information including space ID
     * @see <a href="https://developer.atlassian.com/cloud/confluence/rest/v2/api-group-space/#api-spaces-get">Confluence REST API - Get spaces</a>
     */
    @RequestLine("GET /spaces?keys={spaceKey}")
    fun getSpaceByKey(@Param("spaceKey") spaceKey: String): SpaceListResponse

    /**
     * Get pages in a space
     *
     * @param spaceId The space ID
     * @param cursor Pagination cursor
     * @param limit Maximum number of results (default: 100)
     * @return List of pages
     * @see <a href="https://developer.atlassian.com/cloud/confluence/rest/v2/api-group-page/#api-spaces-id-pages-get">Confluence REST API - Get pages in space</a>
     */
    @RequestLine("GET /spaces/{spaceId}/pages?limit={limit}&cursor={cursor}&body-format=storage")
    fun getPagesBySpaceId(
        @Param("spaceId") spaceId: String,
        @Param("cursor") cursor: String? = null,
        @Param("limit") limit: Int = 100
    ): PageListResponse

    /**
     * Get folder by ID
     *
     * @param folderId The folder ID
     * @return Folder information
     * @see <a href="https://developer.atlassian.com/cloud/confluence/rest/v2/api-group-folder/#api-folders-id-get">Confluence REST API - Get folder</a>
     */
    @RequestLine("GET /folders/{folderId}")
    fun getFolderById(@Param("folderId") folderId: String): FolderResponse

    /**
     * Get page by ID
     *
     * @param pageId The page ID
     * @return Page information including body content
     * @see <a href="https://developer.atlassian.com/cloud/confluence/rest/v2/api-group-page/#api-pages-id-get">Confluence REST API - Get page</a>
     */
    @RequestLine("GET /pages/{pageId}?body-format=storage")
    fun getPageById(@Param("pageId") pageId: String): Page

    /**
     * Get children pages of a page
     *
     * @param pageId The parent page ID
     * @param cursor Pagination cursor
     * @param limit Maximum number of results (default: 100)
     * @return List of child pages
     * @see <a href="https://developer.atlassian.com/cloud/confluence/rest/v2/api-group-page/#api-pages-id-children-get">Confluence REST API - Get page children</a>
     */
    @RequestLine("GET /pages/{pageId}/children?limit={limit}&cursor={cursor}&body-format=storage")
    fun getPageChildren(
        @Param("pageId") pageId: String,
        @Param("cursor") cursor: String? = null,
        @Param("limit") limit: Int = 100
    ): PageListResponse

    /**
     * Get direct children (pages and folders) of a folder
     *
     * @param folderId The parent folder ID
     * @param cursor Pagination cursor
     * @param limit Maximum number of results (default: 100)
     * @return List of child pages and folders
     * @see <a href="https://developer.atlassian.com/cloud/confluence/rest/v2/api-group-children/#api-folders-id-direct-children-get">Confluence REST API - Get folder direct children</a>
     */
    @RequestLine("GET /folders/{folderId}/direct-children?limit={limit}&cursor={cursor}&body-format=storage")
    fun getFolderChildren(
        @Param("folderId") folderId: String,
        @Param("cursor") cursor: String? = null,
        @Param("limit") limit: Int = 100
    ): PageListResponse

    /**
     * Get attachments for a page
     *
     * @param pageId The page ID
     * @param cursor Pagination cursor
     * @param limit Maximum number of results (default: 100)
     * @return List of attachments
     * @see <a href="https://developer.atlassian.com/cloud/confluence/rest/v2/api-group-attachment/#api-pages-id-attachments-get">Confluence REST API - Get attachments</a>
     */
    @RequestLine("GET /pages/{pageId}/attachments?limit={limit}&cursor={cursor}")
    fun getPageAttachments(
        @Param("pageId") pageId: String,
        @Param("cursor") cursor: String? = null,
        @Param("limit") limit: Int = 100
    ): AttachmentListResponse

    /**
     * Download attachment
     *
     * @param attachmentId The attachment ID
     * @return Attachment binary data as byte array
     * @see <a href="https://developer.atlassian.com/cloud/confluence/rest/v2/api-group-attachment/#api-attachments-id-download-get">Confluence REST API - Download attachment</a>
     */
    @RequestLine("GET /attachments/{attachmentId}/download")
    fun downloadAttachment(@Param("attachmentId") attachmentId: String): ByteArray
}
