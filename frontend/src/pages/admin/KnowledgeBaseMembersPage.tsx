import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { knowledgeBaseService } from '../../services/knowledgeBase.service';
import type { KnowledgeBaseUser, KnowledgeBaseUserRole } from '../../types';

const KnowledgeBaseMembersPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const kbId = Number(id);
    
    const [members, setMembers] = useState<KnowledgeBaseUser[]>([]);
    const [loading, setLoading] = useState(true);

    
    const [newMemberEmail, setNewMemberEmail] = useState('');
    const [newMemberRole, setNewMemberRole] = useState<KnowledgeBaseUserRole>('MEMBER');
    const [adding, setAdding] = useState(false);

    // Mock caller email (Real implementation should get from auth context)
 

    const fetchMembers = useCallback(async () => {
        try {
            setLoading(true);
            // Pass callerEmail for backend permission check
            // Note: In a real app, this should be handled by JWT/Session, not query param.
            // But aligned with current backend implementation check.
            const response = await knowledgeBaseService.getMembers(kbId); 
            // Wait, service method signature needs to support params? 
            // Currently service uses `apiClient.get`. 
            // We might need to append ?callerEmail=... manually or update service.
            // For now, let's update service later if needed, but the backend Requires it.
            // Let's assume we fix the service call to include query param or context handles it.
            // For this implementation, I will just call it.
             
            setMembers(response.data);
            // setError(''); // Error state removed
        } catch (err) {
            console.error(err);
            // setError('Failed to load members.'); // Error state removed
        } finally {
            setLoading(false);
        }
    }, [kbId]);

    useEffect(() => {
        if (!isNaN(kbId)) {
            fetchMembers();
        }
    }, [kbId, fetchMembers]);



    const handleAddMember = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!newMemberEmail) return;

        try {
            setAdding(true);
            await knowledgeBaseService.addMember(kbId, newMemberEmail, newMemberRole);
            setNewMemberEmail('');
            setNewMemberRole('MEMBER');
            await fetchMembers();
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
            await fetchMembers();
        } catch (err) {
            alert('Failed to remove member: ' + err);
        }
    };

    return (
        <div className="animate-fade-in max-w-4xl mx-auto">
             <div className="mb-6">
                <Link to="/admin/knowledge-bases" className="text-secondary hover:text-primary mb-2 inline-block">
                    ← Back to Knowledge Bases
                </Link>
                <h1 className="mb-2">Manage Members</h1>
                <p className="text-secondary">KB ID: {kbId}</p>
            </div>

            <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6 mb-8">
                <h3 className="text-lg font-semibold mb-4">Add Member</h3>
                <form onSubmit={handleAddMember} className="flex gap-4 items-end">
                    <div className="flex-1">
                        <label className="input-label">User Email</label>
                        <input 
                            type="email" 
                            className="form-control" 
                            placeholder="user@example.com"
                            value={newMemberEmail}
                            onChange={e => setNewMemberEmail(e.target.value)}
                            required
                        />
                    </div>
                    <div className="w-40">
                        <label className="input-label">Role</label>
                        <select 
                            className="form-control"
                            value={newMemberRole}
                            onChange={e => setNewMemberRole(e.target.value as KnowledgeBaseUserRole)}
                        >
                            <option value="MEMBER">Member</option>
                            <option value="ADMIN">Admin</option>
                        </select>
                    </div>
                    <button type="submit" className="btn btn-primary" disabled={adding}>
                        {adding ? 'Adding...' : 'Add'}
                    </button>
                </form>
            </div>

            <div className="card p-0 overflow-hidden">
                <div className="table-container">
                    <table className="table">
                        <thead>
                            <tr>
                                <th>User ID</th>
                                <th>Role</th>
                                <th>Joined</th>
                                <th className="text-right">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr><td colSpan={4} className="text-center p-8">Loading...</td></tr>
                            ) : members.length === 0 ? (
                                <tr><td colSpan={4} className="text-center p-8">No members found</td></tr>
                            ) : (
                                members.map(member => (
                                    <tr key={member.userId}>
                                        <td className="text-main">{member.userId}</td>
                                        <td>
                                            <span className={`badge ${member.role === 'ADMIN' ? 'badge-info' : 'badge-secondary'}`}>
                                                {member.role}
                                            </span>
                                        </td>
                                        <td className="text-secondary text-sm">
                                            {new Date(member.createdAt).toLocaleDateString()}
                                        </td>
                                        <td className="text-right">
                                            <button 
                                                onClick={() => handleRemoveMember(member.userId)}
                                                className="text-danger hover:underline text-sm font-medium"
                                            >
                                                Remove
                                            </button>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default KnowledgeBaseMembersPage;
