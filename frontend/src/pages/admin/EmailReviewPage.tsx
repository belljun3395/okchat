import React, { useState, useEffect } from 'react';
import { emailService, type PendingEmailReply, type EmailStatus } from '../../services';
import { marked } from 'marked';
import DOMPurify from 'dompurify';


const EmailReviewPage: React.FC = () => {
    const [emails, setEmails] = useState<PendingEmailReply[]>([]);

    const [currentStatus, setCurrentStatus] = useState<EmailStatus>('PENDING');
    const [loading, setLoading] = useState(true);
    const [expandedPreviews, setExpandedPreviews] = useState<number[]>([]);

    // Modal States
    const [selectedEmail, setSelectedEmail] = useState<PendingEmailReply | null>(null);
    const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
    const [isRejectModalOpen, setIsRejectModalOpen] = useState(false);
    const [rejectReason, setRejectReason] = useState('');
    const [emailToReject, setEmailToReject] = useState<number | null>(null);

    useEffect(() => {
        loadData();
        const interval = setInterval(loadData, 30000); // Auto refresh every 30s
        return () => clearInterval(interval);
    }, [currentStatus]);

    const loadData = () => {
        loadEmails(currentStatus);
    };



    const loadEmails = async (status: EmailStatus) => {
        setLoading(true);
        try {
            const response = await emailService.getByStatus(status);
            setEmails(response.data);
        } catch (error) {
            console.error('Failed to load emails:', error);
            setEmails([]);
        } finally {
            setLoading(false);
        }
    };

    const togglePreview = (id: number) => {
        setExpandedPreviews(prev =>
            prev.includes(id) ? prev.filter(p => p !== id) : [...prev, id]
        );
    };

    const handleViewDetail = async (id: number) => {
        try {
            const response = await emailService.getById(id);
            setSelectedEmail(response.data);
            setIsDetailModalOpen(true);
        } catch (error) {
            console.error('Failed to load email detail:', error);
            alert('Failed to load email details.');
        }
    };

    const handleApprove = async (id: number) => {
        if (!window.confirm('Are you sure you want to approve and send this email?')) return;

        try {
            const result = await emailService.approve(id, 'admin');
            if (result.data.success) {
                loadData();
                if (isDetailModalOpen) setIsDetailModalOpen(false);
            } else {
                alert('Failed: ' + result.data.message);
            }
        } catch (error) {
            console.error('Failed to approve email:', error);
            alert('Error approving email.');
        }
    };

    const openRejectModal = (id: number) => {
        setEmailToReject(id);
        setRejectReason('');
        setIsRejectModalOpen(true);
    };

    const handleReject = async () => {
        if (!emailToReject) return;

        try {
            const result = await emailService.reject(emailToReject, {
                reviewedBy: 'admin',
                rejectionReason: rejectReason || undefined
            });

            if (result.data.success) {
                setIsRejectModalOpen(false);
                setEmailToReject(null);
                loadData();
                if (isDetailModalOpen) setIsDetailModalOpen(false);
            } else {
                alert('Failed: ' + result.data.message);
            }
        } catch (error) {
            console.error('Failed to reject email:', error);
            alert('Error rejecting email.');
        }
    };

    const getStatusBadgeColor = (status: string) => {
        switch (status) {
            case 'PENDING': return 'warning';
            case 'APPROVED': return 'success';
            case 'REJECTED': return 'danger';
            case 'SENT': return 'info';
            case 'FAILED': return 'danger';
            default: return 'secondary';
        }
    };

    return (
        <div className="animate-fade-in">
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="mb-2">Email Review</h1>
                    <p className="text-secondary">Review, approve, or reject pending email responses</p>
                </div>
            </div>



            {/* Main Content */}
            <div className="card p-0 overflow-hidden">
                <div className="p-4 border-b bg-gray-50 flex justify-between items-center">
                    <h3 className="m-0 text-base font-semibold">Email Queue</h3>
                    <div className="flex gap-sm">
                        {(['PENDING', 'APPROVED', 'SENT', 'REJECTED', 'FAILED'] as EmailStatus[]).map(status => (
                            <button
                                key={status}
                                className={`btn btn-sm ${currentStatus === status ? 'btn-primary' : 'btn-secondary'}`}
                                onClick={() => setCurrentStatus(status)}
                            >
                                {status.charAt(0) + status.slice(1).toLowerCase()}
                            </button>
                        ))}
                    </div>
                </div>

                <div className="email-list" style={{ maxHeight: '70vh', overflowY: 'auto' }}>
                    {loading ? (
                        <div className="p-8 text-center text-muted">Loading...</div>
                    ) : emails.length === 0 ? (
                        <div className="p-12 text-center text-muted">
                            <div>No emails found with this status</div>
                        </div>
                    ) : (
                        emails.map(email => (
                            <div key={email.id} className="p-6 border-b border-gray-200 hover:bg-gray-50 transition-colors">
                                <div className="flex justify-between items-start mb-2">
                                    <div>
                                        <div className="font-bold text-lg mb-1 text-main">From: {email.fromEmail}</div>
                                        <div className="text-secondary text-sm">Subject: {email.originalSubject}</div>
                                    </div>
                                    <div className="text-right">
                                        <span className={`badge badge-${getStatusBadgeColor(email.status)} mb-1`}>
                                            {email.status}
                                        </span>
                                        <div className="text-xs text-muted">{new Date(email.createdAt).toLocaleString()}</div>
                                    </div>
                                </div>

                                <div
                                    className={`bg-gray-100 p-4 rounded border border-gray-200 my-4 text-sm text-secondary overflow-hidden relative ${expandedPreviews.includes(email.id) ? '' : 'max-h-20'}`}
                                >
                                    <div 
                                        className="email-content-preview"
                                        dangerouslySetInnerHTML={{ 
                                            __html: DOMPurify.sanitize(marked.parse(email.replyContent) as string) 
                                        }}
                                    />
                                </div>

                                <div className="flex gap-sm mt-4">
                                    {email.status === 'PENDING' && (
                                        <>
                                            <button className="btn btn-primary btn-sm" onClick={() => handleApprove(email.id)}>
                                                Approve & Send
                                            </button>
                                            <button className="btn btn-danger btn-sm" onClick={() => openRejectModal(email.id)}>
                                                Reject
                                            </button>
                                        </>
                                    )}
                                    <button className="btn btn-secondary btn-sm" onClick={() => handleViewDetail(email.id)}>
                                        View Details
                                    </button>
                                    <button className="btn btn-secondary btn-sm" onClick={() => togglePreview(email.id)}>
                                        {expandedPreviews.includes(email.id) ? '↑ Collapse' : '↓ Expand Preview'}
                                    </button>
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </div>

            {/* View Detail Modal */}
            {isDetailModalOpen && selectedEmail && (
                <div className="modal-overlay">
                    <div className="modal-content max-w-3xl p-6">
                        <div className="flex justify-between items-center mb-6 border-b pb-4">
                            <h2 className="text-xl m-0 font-bold">Email Details</h2>
                            <button className="text-gray-500 hover:text-gray-700" onClick={() => setIsDetailModalOpen(false)}>
                                ×
                            </button>
                        </div>

                        <div className="flex flex-col gap-md">
                            <div className="input-group">
                                <label className="input-label">From</label>
                                <div className="form-control bg-gray-50 text-secondary">{selectedEmail.fromEmail}</div>
                            </div>
                            <div className="input-group">
                                <label className="input-label">To</label>
                                <div className="form-control bg-gray-50 text-secondary">{selectedEmail.toEmail}</div>
                            </div>
                            <div className="input-group">
                                <label className="input-label">Subject</label>
                                <div className="form-control bg-gray-50 text-secondary">{selectedEmail.originalSubject}</div>
                            </div>
                            <div className="input-group">
                                <label className="input-label">Original Content</label>
                                <div className="form-control bg-gray-50 text-secondary whitespace-pre-wrap">{selectedEmail.originalContent}</div>
                            </div>
                            <div className="input-group">
                                <label className="input-label">Generated Reply</label>
                                <div className="form-control bg-gray-50 text-secondary review-preview-content"
                                     dangerouslySetInnerHTML={{ 
                                         __html: DOMPurify.sanitize(marked.parse(selectedEmail.replyContent) as string) 
                                     }} 
                                />
                            </div>
                            {selectedEmail.rejectionReason && (
                                <div className="input-group">
                                    <label className="input-label text-danger">Rejection Reason</label>
                                    <div className="form-control bg-red-50 text-danger">{selectedEmail.rejectionReason}</div>
                                </div>
                            )}
                        </div>

                        <div className="mt-6 flex justify-end gap-sm pt-4 border-t">
                            {selectedEmail.status === 'PENDING' && (
                                <>
                                    <button className="btn btn-primary" onClick={() => handleApprove(selectedEmail.id)}>Approve & Send</button>
                                    <button className="btn btn-danger" onClick={() => {
                                        setIsDetailModalOpen(false);
                                        openRejectModal(selectedEmail.id);
                                    }}>Reject</button>
                                </>
                            )}
                            <button className="btn btn-secondary" onClick={() => setIsDetailModalOpen(false)}>Close</button>
                        </div>
                    </div>
                </div>
            )}

            {/* Reject Reason Modal */}
            {isRejectModalOpen && (
                <div className="modal-overlay">
                    <div className="modal-content max-w-md p-6">
                        <h2 className="mb-4 text-xl font-bold">Reject Email</h2>
                        <div className="input-group">
                            <label className="input-label">Reason for rejection (optional)</label>
                            <textarea
                                className="form-control"
                                rows={4}
                                placeholder="Enter reason..."
                                value={rejectReason}
                                onChange={(e) => setRejectReason(e.target.value)}
                            ></textarea>
                        </div>
                        <div className="flex justify-end gap-sm mt-4">
                            <button className="btn btn-secondary" onClick={() => setIsRejectModalOpen(false)}>Cancel</button>
                            <button className="btn btn-danger" onClick={handleReject}>Confirm Rejection</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default EmailReviewPage;
