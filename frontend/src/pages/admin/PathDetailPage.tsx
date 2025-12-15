import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { permissionService } from '../../services';
import type { PathDetailResponse } from '../../types';

/**
 * PathDetailPage Component
 * 
 * Displays details about a specific document path.
 * Shows all documents under this path and users who have access.
 * 
 * API: GET /api/admin/permissions/path/detail
 */
const PathDetailPage: React.FC = () => {
    const [searchParams] = useSearchParams();
    const path = searchParams.get('path');

    const [details, setDetails] = useState<PathDetailResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const fetchPathDetails = useCallback(async () => {
        try {
            setLoading(true);
            const response = await permissionService.getPathDetail(path!);
            setDetails(response.data);
        } catch (err) {
            console.error('Failed to fetch path details:', err);
            setError('Failed to load path details.');
        } finally {
            setLoading(false);
        }
    }, [path]);

    useEffect(() => {
        if (path) {
            fetchPathDetails();
        }
    }, [path, fetchPathDetails]);



    if (!path) return <div className="p-8 text-center text-danger">Invalid path provided</div>;
    if (loading) return <div className="p-8 text-center">Loading...</div>;
    if (error) return <div className="p-8 text-center text-danger">{error}</div>;
    if (!details) return <div className="p-8 text-center">Path not found</div>;

    return (
        <div className="animate-fade-in">
            <div className="mb-6">
                <Link to="/admin/paths" className="text-secondary hover:text-primary mb-4 inline-block">
                    ← Back to Paths
                </Link>
                <h1 className="mb-2 break-all">{details.path}</h1>
                <div className="flex gap-4 text-secondary text-sm">
                    <span>
                        {details.totalDocuments} Documents
                    </span>
                    <span>
                        {details.totalUsers} Users with Access
                    </span>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-lg">
                {/* Documents List */}
                <div className="card">
                    <h2 className="text-xl mb-4">Documents</h2>
                    {details.documents.length === 0 ? (
                        <p className="text-secondary">No documents found in this path.</p>
                    ) : (
                        <div className="space-y-3">
                            {details.documents.map(doc => (
                                <div key={doc.id} className="p-3 bg-gray-50 rounded border border-gray-200">
                                    <div className="flex items-center justify-between mb-1">
                                        <div className="flex items-center gap-2">
                                            <span className="font-medium text-main">{doc.title}</span>
                                            {doc.webUrl && (
                                                <a
                                                    href={doc.webUrl}
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                    className="text-gray-400 hover:text-primary transition-colors p-1 rounded hover:bg-gray-100"
                                                    title="Open original document"
                                                >
                                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                                                    </svg>
                                                </a>
                                            )}
                                        </div>
                                    </div>
                                    <div className="text-xs text-secondary break-all">{doc.url}</div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* Users with Access */}
                <div className="card">
                    <h2 className="text-xl mb-4">Users with Access</h2>
                    {details.usersWithAccess.length === 0 ? (
                        <p className="text-secondary">No users have explicit access to this path.</p>
                    ) : (
                        <div className="table-container">
                            <table className="table">
                                <thead>
                                    <tr>
                                        <th>User</th>
                                        <th>Status</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {details.usersWithAccess.map(user => (
                                        <tr key={user.id}>
                                            <td>
                                                <div className="flex flex-col">
                                                    <span className="font-medium text-main">{user.name}</span>
                                                    <span className="text-xs text-secondary">{user.email}</span>
                                                </div>
                                            </td>
                                            <td>
                                                <span className={`badge ${user.active ? 'badge-success' : 'badge-secondary'}`}>
                                                    {user.active ? 'Active' : 'Inactive'}
                                                </span>
                                            </td>
                                            <td>
                                                <Link
                                                    to={`/admin/permissions/user/${encodeURIComponent(user.email)}`}
                                                    className="text-primary hover:underline text-sm"
                                                >
                                                    Manage
                                                </Link>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default PathDetailPage;
