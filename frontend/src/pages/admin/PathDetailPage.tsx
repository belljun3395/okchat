import React, { useState, useEffect } from 'react';
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

    useEffect(() => {
        if (path) {
            fetchPathDetails();
        }
    }, [path]);

    const fetchPathDetails = async () => {
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
    };

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
                                    <div className="font-medium text-main mb-1">{doc.title}</div>
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
