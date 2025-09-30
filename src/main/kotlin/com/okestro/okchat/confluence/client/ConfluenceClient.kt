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
    @RequestLine("GET /spaces/{spaceId}/pages?limit={limit}&cursor={cursor}")
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
}
