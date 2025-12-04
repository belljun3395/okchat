import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import type { User } from '../../types';

/**
 * UserManagementPage Component
 * 
 * Displays a list of all users in the system.
 * Allows filtering and navigation to user details.
 * 
 * API: GET /admin/users (via UserAdminWebController)
 */
const UserManagementPage: React.FC = () => {
    const [users, setUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [searchTerm, setSearchTerm] = useState('');

    useEffect(() => {
        fetchUsers();
    }, []);

    const fetchUsers = async () => {
        try {
            setLoading(true);
            // Calling existing backend API
            // Note: The controller returns a List<User>
            const response = await axios.get<User[]>('/admin/users');
            setUsers(response.data);
            setError('');
        } catch (err) {
            console.error('Failed to fetch users:', err);
            setError('Failed to load users. Please try again later.');
        } finally {
            setLoading(false);
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
                <button className="btn btn-primary">
                    + Add User
                </button>
            </div>

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
                                            <span className="badge badge-info">User</span>
                                        </td>
                                        <td>
                                            {user.active ? (
                                                <span className="badge badge-success">Active</span>
                                            ) : (
                                                <span className="badge badge-secondary">Inactive</span>
                                            )}
                                        </td>
                                        <td className="text-secondary text-sm">-</td>
                                        <td>
                                            <Link
                                                to={`/admin/permissions/user/${user.email}`}
                                                className="text-primary hover:underline text-sm font-medium"
                                            >
                                                Edit
                                            </Link>
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
