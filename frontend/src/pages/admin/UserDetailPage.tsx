import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { userService, permissionService } from '../../services';
import { ApiError } from '../../lib/api-client';
import type { User, DocumentPathPermission } from '../../types';

/**
 * UserDetailPage Component
 *
 * Displays detailed information about a user and their permissions.
 * Allows granting and revoking path-based permissions.
 *
 * APIs:
 * - GET /admin/users/{email} - Get user info
 * - GET /api/admin/permissions/user/{email} - Get user permissions
 * - POST /api/admin/permissions/path/bulk - Grant permissions
 * - DELETE /api/admin/permissions/path/bulk - Revoke permissions
 */
const UserDetailPage: React.FC = () => {
    const { email: encodedEmail } = useParams<{ email: string }>();
    const email = encodedEmail ? decodeURIComponent(encodedEmail) : undefined;
    const [user, setUser] = useState<User | null>(null);
    const [permissions, setPermissions] = useState<DocumentPathPermission[]>([]);
    const [allPaths, setAllPaths] = useState<string[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // State for adding new permission
    const [selectedPath, setSelectedPath] = useState('');
    const [isAdding, setIsAdding] = useState(false);
    const [showGrantModal, setShowGrantModal] = useState(false);

    const fetchUserDetails = useCallback(async () => {
        try {
            setLoading(true);

            // Fetch user info from correct endpoint
            const userResponse = await userService.getByEmail(email!);
            setUser(userResponse.data);

            // Fetch permissions separately
            const permResponse = await permissionService.getUserPermissions(email!);
            setPermissions(permResponse.data.pathPermissions);

            setError('');
        } catch (err) {
            console.error('Failed to fetch user details:', err);
            if (err instanceof ApiError) {
                setError(err.message);
            } else {
                setError('Failed to load user details.');
            }
        } finally {
            setLoading(false);
        }
    }, [email]);

    const fetchAllPaths = useCallback(async () => {
        try {
            const response = await permissionService.getAllPaths();
            setAllPaths(response.data);
        } catch (err) {
            console.error('Failed to fetch paths:', err);
        }
    }, []);

    useEffect(() => {
        if (email) {
            fetchUserDetails();
            fetchAllPaths();
        }
    }, [email, fetchUserDetails, fetchAllPaths]);





    const handleGrantPermission = async () => {
        if (!selectedPath || !user) return;

        try {
            setIsAdding(true);

            await permissionService.grantBulk({
                userEmail: user.email,
                documentPaths: [selectedPath]
            });

            // Refresh permissions
            await fetchUserDetails();
            setSelectedPath('');
            setShowGrantModal(false);
        } catch (err) {
            console.error('Failed to grant permission:', err);
            const message = err instanceof ApiError ? err.message : 'Failed to grant permission';
            alert(message);
        } finally {
            setIsAdding(false);
        }
    };

    const handleRevokePermission = async (path: string) => {
        if (!user || !window.confirm(`Are you sure you want to revoke access to ${path}?`)) return;

        try {
            await permissionService.revokeBulk({
                userEmail: user.email,
                documentPaths: [path]
            });

            // Refresh permissions
            await fetchUserDetails();
        } catch (err) {
            console.error('Failed to revoke permission:', err);
            const message = err instanceof ApiError ? err.message : 'Failed to revoke permission';
            alert(message);
        }
    };

    if (loading) return <div className="p-8 text-center">Loading...</div>;
    if (error) return <div className="p-8 text-center text-danger">{error}</div>;
    if (!user) return <div className="p-8 text-center">User not found</div>;

    return (
        <div className="animate-fade-in">
            <div className="mb-6">
                <Link to="/admin/users" className="btn btn-secondary btn-sm mb-4 inline-block">
                    ← Back to Users
                </Link>
            </div>

            <div className="grid grid-cols-3 gap-lg">
                {/* User Profile Card */}
                <div className="card">
                    <div className="flex flex-col items-center text-center mb-6">
                        <div className="w-20 h-20 rounded-full bg-blue-100 flex items-center justify-center text-primary text-2xl font-bold mb-4 border border-blue-200">
                            <span>{user.name.charAt(0).toUpperCase()}</span>
                        </div>
                        <h2 className="text-lg font-bold mb-1">{user.name}</h2>
                        <p className="text-sm text-secondary mb-2">{user.email}</p>
                        <div>
                            <span className={`badge ${user.active ? 'badge-success' : 'badge-secondary'}`}>
                                {user.active ? 'Active' : 'Inactive'}
                            </span>
                        </div>
                    </div>

                    <hr className="border-gray-200 my-4" />

                    <div className="flex flex-col gap-md">
                        <div>
                            <div className="text-xs text-secondary uppercase font-bold tracking-wider mb-1">Role</div>
                            <div className="font-medium text-main">{user.role}</div>
                        </div>
                        <div>
                            <div className="text-xs text-secondary uppercase font-bold tracking-wider mb-1">Joined</div>
                            <div className="font-medium text-main">
                                {user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '-'}
                            </div>
                        </div>
                    </div>

                    <div className="mt-6 pt-4 border-t border-gray-200">
                        <button className="btn btn-danger w-full">Deactivate Account</button>
                    </div>
                </div>

                {/* Permissions & Activity */}
                <div className="col-span-2 flex flex-col gap-lg animate-fade-in delay-100" style={{ gridColumn: 'span 2' }}>
                    <div className="card">
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="text-lg font-semibold m-0">Assigned Permissions</h3>
                            <button
                                className="btn btn-secondary btn-sm"
                                onClick={() => setShowGrantModal(true)}
                            >
                                + Grant Permission
                            </button>
                        </div>
                        <div className="table-container">
                            <table className="table">
                                <thead>
                                    <tr>
                                        <th>Path</th>
                                        <th>Access Level</th>
                                        <th>Source</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {permissions.length === 0 ? (
                                        <tr>
                                            <td colSpan={4} className="text-center text-secondary p-4">No permissions assigned</td>
                                        </tr>
                                    ) : (
                                        permissions.map(perm => (
                                            <tr key={perm.id}>
                                                <td className="font-mono text-sm text-main">{perm.documentPath}</td>
                                                <td>
                                                    <span className={`badge ${
                                                        perm.permissionLevel === 'READ' ? 'badge-info' :
                                                        perm.permissionLevel === 'DENY' ? 'badge-danger' :
                                                        perm.permissionLevel === 'WRITE' ? 'badge-success' :
                                                        'badge-secondary'
                                                    }`}>
                                                        {perm.permissionLevel}
                                                    </span>
                                                </td>
                                                <td className="text-sm text-secondary">Direct Assignment</td>
                                                <td>
                                                    <button
                                                        className="text-danger hover:underline text-sm font-medium"
                                                        onClick={() => handleRevokePermission(perm.documentPath)}
                                                    >
                                                        Revoke
                                                    </button>
                                                </td>
                                            </tr>
                                        ))
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <div className="card">
                        <h3 className="mb-4 text-lg font-semibold">Recent Activity</h3>
                        <div className="flex flex-col">
                            <div className="p-4 text-center text-secondary text-sm">
                                No recent activity found.
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Grant Permission Modal */}
            {showGrantModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
                        <h2 className="text-xl font-semibold mb-4">Grant Permission</h2>
                        <div className="space-y-4">
                            <div>
                                <label className="input-label">Select Path</label>
                                <select
                                    className="form-control"
                                    value={selectedPath}
                                    onChange={(e) => setSelectedPath(e.target.value)}
                                >
                                    <option value="">-- Select a path --</option>
                                    {allPaths.map(path => (
                                        <option key={path} value={path}>{path}</option>
                                    ))}
                                </select>
                            </div>
                        </div>
                        <div className="flex gap-2 mt-6">
                            <button
                                className="btn btn-secondary flex-1"
                                onClick={() => {
                                    setShowGrantModal(false);
                                    setSelectedPath('');
                                }}
                                disabled={isAdding}
                            >
                                Cancel
                            </button>
                            <button
                                className="btn btn-primary flex-1"
                                onClick={handleGrantPermission}
                                disabled={!selectedPath || isAdding}
                            >
                                {isAdding ? 'Granting...' : 'Grant Permission'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default UserDetailPage;
