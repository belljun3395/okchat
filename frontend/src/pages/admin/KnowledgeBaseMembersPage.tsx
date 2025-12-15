import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { knowledgeBaseService, KnowledgeBaseMember } from '../../services/knowledgeBase.service';
import type { KnowledgeBaseUserRole, KnowledgeBase } from '../../types';

const KnowledgeBaseMembersPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const kbId = Number(id);

    // State
    const [members, setMembers] = useState<KnowledgeBaseMember[]>([]);
    const [currentKb, setCurrentKb] = useState<KnowledgeBase | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // Modal State
    const [showAddModal, setShowAddModal] = useState(false);
    const [newMemberEmail, setNewMemberEmail] = useState('');
    const [newMemberRole, setNewMemberRole] = useState<KnowledgeBaseUserRole>('MEMBER');
    const [adding, setAdding] = useState(false);

    // Edit Modal State
    const [showEditModal, setShowEditModal] = useState(false);
    const [editingMember, setEditingMember] = useState<KnowledgeBaseMember | null>(null);
    const [editRole, setEditRole] = useState<KnowledgeBaseUserRole>('MEMBER');
    const [updating, setUpdating] = useState(false);

    const fetchMembersAndKb = useCallback(async () => {
        try {
            setLoading(true);
            const [membersResponse, kbsResponse] = await Promise.all([
                knowledgeBaseService.getMembers(kbId),
                knowledgeBaseService.getAll()
            ]);

            setMembers(membersResponse.data);

            // Find current KB details
            const foundKb = kbsResponse.data.find(k => k.id === kbId);
            if (foundKb) {
                setCurrentKb(foundKb);
            }

            setError('');
        } catch (err) {
            console.error(err);
            setError('Failed to load data.');
        } finally {
            setLoading(false);
        }
    }, [kbId]);

    useEffect(() => {
        if (!isNaN(kbId)) {
            fetchMembersAndKb();
        }
    }, [kbId, fetchMembersAndKb]);

    const handleAddMember = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!newMemberEmail) return;

        try {
            setAdding(true);
            await knowledgeBaseService.addMember(kbId, newMemberEmail, newMemberRole);
            setShowAddModal(false);
            setNewMemberEmail('');
            setNewMemberRole('MEMBER');

            // Refresh members list
            const response = await knowledgeBaseService.getMembers(kbId);
            setMembers(response.data);
        } catch (err) {
            alert('Failed to add member: ' + err);
        } finally {
            setAdding(false);
        }
    };

    const handleRemoveMember = async (userId: number) => {
        if (!confirm('Remove this member?')) return;
        try {
            await knowledgeBaseService.removeMember(kbId, userId);
            // Refresh members list
            const response = await knowledgeBaseService.getMembers(kbId);
            setMembers(response.data);
        } catch (err) {
            alert('Failed to remove member: ' + err);
        }
    };

    const openEditModal = (member: KnowledgeBaseMember) => {
        setEditingMember(member);
        setEditRole(member.role);
        setShowEditModal(true);
    };

    const handleUpdateRole = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!editingMember) return;

        try {
            setUpdating(true);
            await knowledgeBaseService.updateMemberRole(kbId, editingMember.userId, editRole);
            setShowEditModal(false);
            setEditingMember(null);

            // Refresh members list
            const response = await knowledgeBaseService.getMembers(kbId);
            setMembers(response.data);
        } catch (err) {
            alert('Failed to update role: ' + err);
        } finally {
            setUpdating(false);
        }
    };

    return (
        <div className="animate-fade-in">
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="mb-2">Knowledge Base Members {currentKb ? `(${currentKb.name})` : (kbId ? `(KB #${kbId})` : '')}</h1>
                    <p className="text-secondary">
                        Manage members and their roles for this Knowledge Base
                    </p>
                </div>
                <div className="flex gap-4">
                    <Link to="/admin" className="btn btn-secondary">
                        Back to Dashboard
                    </Link>
                    <button
                        className="btn btn-primary"
                        onClick={() => setShowAddModal(true)}
                    >
                        + Add Member
                    </button>
                </div>
            </div>

            <div className="card p-0 overflow-hidden">
                <div className="table-container">
                    <table className="table">
                        <thead>
                            <tr>
                                <th>User</th>
                                <th>Role</th>
                                <th>Approved By</th>
                                <th>Joined</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr><td colSpan={5} className="text-center p-8">Loading...</td></tr>
                            ) : error ? (
                                <tr><td colSpan={5} className="text-center p-8 text-danger">{error}</td></tr>
                            ) : members.length === 0 ? (
                                <tr><td colSpan={5} className="text-center p-8">No members found</td></tr>
                            ) : (
                                members.map(member => (
                                    <tr key={member.userId}>
                                        <td className="text-main">
                                            <div className="font-medium">{member.name}</div>
                                            <div className="text-xs text-secondary">{member.email}</div>
                                        </td>
                                        <td>
                                            <span className={`badge ${member.role === 'ADMIN' ? 'badge-info' : 'badge-secondary'}`}>
                                                {member.role}
                                            </span>
                                        </td>
                                        <td className="text-secondary text-sm">
                                            {member.approvedBy || '-'}
                                        </td>
                                        <td className="text-secondary text-sm">
                                            {new Date(member.createdAt).toLocaleDateString()}
                                        </td>
                                        <td>
                                            <div className="flex gap-2">
                                                <Link
                                                    to={`/admin/permissions/user/${encodeURIComponent(member.email)}`}
                                                    className="btn btn-sm btn-secondary"
                                                >
                                                    Permissions
                                                </Link>
                                                <button
                                                    onClick={() => openEditModal(member)}
                                                    className="btn btn-sm btn-outline"
                                                >
                                                    Edit Role
                                                </button>
                                                <button
                                                    onClick={() => handleRemoveMember(member.userId)}
                                                    className="btn btn-sm btn-danger"
                                                >
                                                    Remove
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Add Member Modal */}
            {showAddModal && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 animate-fade-in">
                    <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
                        <h2 className="text-xl font-bold mb-4">Add Member</h2>
                        <form onSubmit={handleAddMember}>
                            <div className="mb-4">
                                <label className="block text-sm font-medium mb-1">User Email</label>
                                <input
                                    type="email"
                                    className="form-control w-full"
                                    placeholder="user@example.com"
                                    value={newMemberEmail}
                                    onChange={e => setNewMemberEmail(e.target.value)}
                                    required
                                />
                            </div>
                            <div className="mb-4">
                                <label className="block text-sm font-medium mb-1">Role</label>
                                <select
                                    className="form-control w-full"
                                    value={newMemberRole}
                                    onChange={e => setNewMemberRole(e.target.value as KnowledgeBaseUserRole)}
                                >
                                    <option value="MEMBER">Member</option>
                                    <option value="ADMIN">Admin</option>
                                </select>
                            </div>
                            <div className="flex justify-end gap-2 mt-6">
                                <button
                                    type="button"
                                    className="btn btn-secondary"
                                    onClick={() => setShowAddModal(false)}
                                    disabled={adding}
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    className="btn btn-primary"
                                    disabled={adding}
                                >
                                    {adding ? 'Adding...' : 'Add'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Edit Member Role Modal */}
            {showEditModal && editingMember && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 animate-fade-in">
                    <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
                        <h2 className="text-xl font-bold mb-4">Edit Member Role</h2>
                        <form onSubmit={handleUpdateRole}>
                            <div className="mb-4">
                                <label className="block text-sm font-medium mb-1">Member</label>
                                <div className="p-3 bg-gray-50 rounded-lg">
                                    <div className="font-medium">{editingMember.name}</div>
                                    <div className="text-sm text-secondary">{editingMember.email}</div>
                                </div>
                            </div>
                            <div className="mb-4">
                                <label className="block text-sm font-medium mb-1">Role</label>
                                <select
                                    className="form-control w-full"
                                    value={editRole}
                                    onChange={e => setEditRole(e.target.value as KnowledgeBaseUserRole)}
                                >
                                    <option value="MEMBER">Member</option>
                                    <option value="ADMIN">Admin</option>
                                </select>
                            </div>
                            <div className="flex justify-end gap-2 mt-6">
                                <button
                                    type="button"
                                    className="btn btn-secondary"
                                    onClick={() => setShowEditModal(false)}
                                    disabled={updating}
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    className="btn btn-primary"
                                    disabled={updating}
                                >
                                    {updating ? 'Saving...' : 'Save'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default KnowledgeBaseMembersPage;
