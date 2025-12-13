import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { userService } from '../../services';
import { ApiError } from '../../lib/api-client';
import type { User } from '../../types';

/**
 * UserManagementPage Component
 *
 * Displays a list of all users in the system.
 * Allows filtering, creating, and deactivating users.
 *
 * APIs:
 * - GET /admin/users - Get all active users
 * - POST /admin/users - Create user
 * - DELETE /admin/users/{email} - Deactivate user
 */
const UserManagementPage: React.FC = () => {
    const [users, setUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [searchTerm, setSearchTerm] = useState('');

    // Add user modal state
    const [showAddModal, setShowAddModal] = useState(false);
    const [newUser, setNewUser] = useState({ email: '', name: '' });
    const [isCreating, setIsCreating] = useState(false);

    useEffect(() => {
        fetchUsers();
    }, []);

    const fetchUsers = async () => {
        try {
            setLoading(true);
            const response = await userService.getAll();
            setUsers(response.data);
            setError('');
        } catch (err) {
            console.error('Failed to fetch users:', err);
            if (err instanceof ApiError) {
                setError(err.message);
            } else {
                setError('Failed to load users. Please try again later.');
            }
        } finally {
            setLoading(false);
        }
    };

    const handleAddUser = async () => {
        if (!newUser.email || !newUser.name) {
            alert('Please enter both email and name');
            return;
        }

        try {
            setIsCreating(true);
            await userService.create(newUser);
            setShowAddModal(false);
            setNewUser({ email: '', name: '' });
            await fetchUsers();
        } catch (err) {
            console.error('Failed to create user:', err);
            const message = err instanceof ApiError ? err.message : 'Failed to create user';
            alert(message);
        } finally {
            setIsCreating(false);
        }
    };

    const handleDeactivateUser = async (email: string, name: string) => {
        if (!window.confirm(`Are you sure you want to deactivate user ${name} (${email})?`)) {
            return;
        }

        try {
            await userService.deactivate(email);
            await fetchUsers();
        } catch (err) {
            console.error('Failed to deactivate user:', err);
            const message = err instanceof ApiError ? err.message : 'Failed to deactivate user';
            alert(message);
        }
    };

    // Filter users based on search term
    const filteredUsers = users.filter(user =>
        user.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        user.email.toLowerCase().includes(searchTerm.toLowerCase())
    );

    return (
        <div className="animate-fade-in">
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="mb-2">Manage Users</h1>
                    <p className="text-secondary">View and manage user access rights</p>
                </div>
                <button className="btn btn-primary" onClick={() => setShowAddModal(true)}>
                    + Add User
                </button>
            </div>

            {/* Add User Modal */}
            {showAddModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
                        <h2 className="text-xl font-semibold mb-4">Add New User</h2>
                        <div className="space-y-4">
                            <div>
                                <label className="input-label">Email</label>
                                <input
                                    type="email"
                                    className="form-control"
                                    placeholder="user@example.com"
                                    value={newUser.email}
                                    onChange={(e) => setNewUser({ ...newUser, email: e.target.value })}
                                />
                            </div>
                            <div>
                                <label className="input-label">Name</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    placeholder="John Doe"
                                    value={newUser.name}
                                    onChange={(e) => setNewUser({ ...newUser, name: e.target.value })}
                                />
                            </div>
                        </div>
                        <div className="flex gap-2 mt-6">
                            <button
                                className="btn btn-secondary flex-1"
                                onClick={() => {
                                    setShowAddModal(false);
                                    setNewUser({ email: '', name: '' });
                                }}
                                disabled={isCreating}
                            >
                                Cancel
                            </button>
                            <button
                                className="btn btn-primary flex-1"
                                onClick={handleAddUser}
                                disabled={isCreating}
                            >
                                {isCreating ? 'Creating...' : 'Create User'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <div className="card p-0 overflow-hidden">
                {/* Toolbar */}
                <div className="p-4 border-b bg-gray-50 flex gap-md items-center">
                    <div className="relative flex-1 max-w-xs">
                        <input
                            type="text"
                            className="form-control"
                            placeholder="Search users..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                    <select className="form-control" style={{ width: 'auto' }}>
                        <option>All Roles</option>
                        <option>Admin</option>
                        <option>User</option>
                    </select>
                </div>

                {/* Table */}
                <div className="table-container">
                    <table className="table">
                        <thead>
                            <tr>
                                <th>User Info</th>
                                <th>Role</th>
                                <th>Status</th>
                                <th>Last Active</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr>
                                    <td colSpan={5} className="text-center p-8 text-secondary">Loading users...</td>
                                </tr>
                            ) : error ? (
                                <tr>
                                    <td colSpan={5} className="text-center p-8 text-danger">{error}</td>
                                </tr>
                            ) : filteredUsers.length === 0 ? (
                                <tr>
                                    <td colSpan={5} className="text-center p-8 text-secondary">No users found</td>
                                </tr>
                            ) : (
                                filteredUsers.map(user => (
                                    <tr key={user.id}>
                                        <td>
                                            <div className="flex flex-col">
                                                <span className="font-medium text-main">{user.name}</span>
                                                <span className="text-sm text-secondary">{user.email}</span>
                                            </div>
                                        </td>
                                        <td>
                                            <span className={`badge ${
                                                user.role === 'SYSTEM_ADMIN' ? 'badge-primary' : 'badge-info'
                                            }`}>{user.role}</span>
                                        </td>
                                        <td>
                                            {user.active ? (
                                                <span className="badge badge-success">Active</span>
                                            ) : (
                                                <span className="badge badge-secondary">Inactive</span>
                                            )}
                                        </td>
                                        <td className="text-secondary text-sm">
                                            {user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '-'}
                                        </td>
                                        <td>
                                            <div className="flex gap-2">
                                                <Link
                                                    to={`/admin/permissions/user/${encodeURIComponent(user.email)}`}
                                                    className="btn btn-secondary btn-sm"
                                                >
                                                    Edit
                                                </Link>
                                                <button
                                                    onClick={() => handleDeactivateUser(user.email, user.name)}
                                                    className="btn btn-danger btn-sm"
                                                >
                                                    Deactivate
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>

                {/* Footer */}
                <div className="p-4 border-t bg-gray-50 flex justify-between items-center text-sm text-secondary">
                    <span>Showing {filteredUsers.length} users</span>
                    <div className="flex gap-sm">
                        <button className="btn btn-secondary btn-sm" disabled>Previous</button>
                        <button className="btn btn-secondary btn-sm" disabled>Next</button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default UserManagementPage;
