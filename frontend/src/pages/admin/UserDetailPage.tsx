import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import axios from 'axios';
import type { User, DocumentPathPermission } from '../../types';

/**
 * UserDetailPage Component
 * 
 * Displays detailed information about a user and their permissions.
 * Allows granting and revoking path-based permissions.
 * 
 * APIs:
 * - GET /api/admin/permissions/user/{email}
 * - DELETE /api/admin/permissions/user/{email} (Revoke all)
 * - POST /api/admin/permissions/path/bulk (Grant)
 * - DELETE /api/admin/permissions/path/bulk (Revoke)
 */
const UserDetailPage: React.FC = () => {
    const { email } = useParams<{ email: string }>();
    const [user, setUser] = useState<User | null>(null);
    const [permissions, setPermissions] = useState<DocumentPathPermission[]>([]);
    const [allPaths, setAllPaths] = useState<string[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // State for adding new permission
    const [selectedPath, setSelectedPath] = useState('');
    const [isAdding, setIsAdding] = useState(false);

    useEffect(() => {
        if (email) {
            fetchUserDetails();
            fetchAllPaths();
        }
    }, [email]);

    const fetchUserDetails = async () => {
        try {
            setLoading(true);
            const response = await axios.get<{ user: User; pathPermissions: DocumentPathPermission[] }>(`/api/admin/permissions/user/${email}`);
            setUser(response.data.user);
            setPermissions(response.data.pathPermissions);
        } catch (err) {
            console.error('Failed to fetch user details:', err);
            setError('Failed to load user details.');
        } finally {
            setLoading(false);
        }
    };

    const fetchAllPaths = async () => {
        try {
            const response = await axios.get<string[]>('/api/admin/permissions/paths');
            setAllPaths(response.data);
        } catch (err) {
            console.error('Failed to fetch paths:', err);
        }
    };

    const handleGrantPermission = async () => {
        if (!selectedPath || !user) return;

        try {
            setIsAdding(true);
            await axios.post('/api/admin/permissions/path/bulk', {
                userEmail: user.email,
                documentPaths: [selectedPath]
            });

            // Refresh permissions
            await fetchUserDetails();
            setSelectedPath('');
        } catch (err) {
            console.error('Failed to grant permission:', err);
            alert('Failed to grant permission');
        } finally {
            setIsAdding(false);
        }
    };

    const handleRevokePermission = async (path: string) => {
        if (!user || !window.confirm(`Are you sure you want to revoke access to ${path}?`)) return;

        try {
            await axios.delete('/api/admin/permissions/path/bulk', {
                data: {
                    userEmail: user.email,
                    documentPaths: [path]
                }
            });

            // Refresh permissions
            await fetchUserDetails();
        } catch (err) {
            console.error('Failed to revoke permission:', err);
            alert('Failed to revoke permission');
        }
    };

    if (loading) return <div className="p-8 text-center">Loading...</div>;
    if (error) return <div className="p-8 text-center text-danger">{error}</div>;
    if (!user) return <div className="p-8 text-center">User not found</div>;

    return (
        <div className="animate-fade-in">
            <div className="mb-6">
                <Link to="/admin/users" className="text-secondary hover:text-primary mb-4 inline-block">
                    ← Back to Users
                </Link>
                <div className="flex justify-between items-start">
                    <div>
                        <h1 className="mb-2">{user.name}</h1>
                        <p className="text-secondary">{user.email}</p>
                    </div>
                    <div className="flex gap-2">
                        <span className={`badge ${user.active ? 'badge-success' : 'badge-secondary'}`}>
                            {user.active ? 'Active' : 'Inactive'}
                        </span>
                        <span className="badge badge-info">User</span>
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 gap-lg">
                {/* Permissions Card */}
                <div className="card">
                    <div className="flex justify-between items-center mb-6">
                        <h2 className="text-xl mb-0">Path Permissions</h2>

                        {/* Add Permission Form */}
                        <div className="flex gap-2">
                            <select
                                className="form-control"
                                style={{ width: '300px' }}
                                value={selectedPath}
                                onChange={(e) => setSelectedPath(e.target.value)}
                            >
                                <option value="">Select path to grant access...</option>
                                {allPaths.map(path => (
                                    <option key={path} value={path}>{path}</option>
                                ))}
                            </select>
                            <button
                                className="btn btn-primary"
                                onClick={handleGrantPermission}
                                disabled={!selectedPath || isAdding}
                            >
                                Grant
                            </button>
                        </div>
                    </div>

                    {permissions.length === 0 ? (
                        <div className="text-center p-8 bg-gray-50 rounded-lg border border-dashed border-gray-300">
                            <p className="mb-0">No explicit path permissions assigned.</p>
                        </div>
                    ) : (
                        <div className="table-container">
                            <table className="table">
                                <thead>
                                    <tr>
                                        <th>Path</th>
                                        <th>Type</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {permissions.map(perm => (
                                        <tr key={perm.id}>
                                            <td className="font-medium">{perm.path}</td>
                                            <td>
                                                <span className={`badge ${perm.permissionType === 'GRANT' ? 'badge-success' : 'badge-danger'}`}>
                                                    {perm.permissionType}
                                                </span>
                                            </td>
                                            <td>
                                                <button
                                                    className="text-danger hover:text-red-700 text-sm font-medium"
                                                    onClick={() => handleRevokePermission(perm.path)}
                                                >
                                                    Revoke
                                                </button>
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

export default UserDetailPage;
