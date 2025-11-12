import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { complaintService } from "../services/complaintService";
import { Complaint, ComplaintStatus, ComplaintPriority } from "../types";
import CommentSection from "../components/complaints/CommentSection";
import ComplaintEditModal from "../components/complaints/ComplaintEditModal";
import {
  ArrowLeft,
  Calendar,
  User,
  AlertCircle,
  Clock,
  CheckCircle,
  XCircle,
  MoreHorizontal,
  Edit,
  MessageSquare,
} from "lucide-react";
import { getDepartmentDisplayName } from "../utils/departmentUtils";
import { statusDisplay } from "../utils/statusUtils";

const ComplaintDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [complaint, setComplaint] = useState<Complaint | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);

  useEffect(() => {
    if (id) {
      fetchComplaintDetails();
    }
  }, [id]);

  const fetchComplaintDetails = async () => {
    try {
      setLoading(true);
      const complaintData = await complaintService.getComplaintById(id!);
      setComplaint(complaintData);
    } catch (err: any) {
      setError("Failed to load complaint details");
      console.error("Error loading complaint:", err);
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: ComplaintStatus) => {
    switch (status) {
      case ComplaintStatus.CREATED:
        return "bg-blue-100 text-blue-800";
      case ComplaintStatus.IN_PROGRESS:
        return "bg-yellow-100 text-yellow-800";
      case ComplaintStatus.ASSIGNED:
        return "bg-indigo-100 text-indigo-800";
      case ComplaintStatus.BLOCKED:
        return "bg-orange-100 text-orange-800";
      case ComplaintStatus.RESOLVED:
        return "bg-green-100 text-green-800";
      case ComplaintStatus.CLOSED:
        return "bg-gray-100 text-gray-800";
      default:
        return "bg-gray-100 text-gray-800";
    }
  };

  const getPriorityColor = (priority: ComplaintPriority) => {
    switch (priority) {
      case ComplaintPriority.URGENT:
        return "bg-red-100 text-red-800";
      case ComplaintPriority.HIGH:
        return "bg-orange-100 text-orange-800";
      case ComplaintPriority.MEDIUM:
        return "bg-yellow-100 text-yellow-800";
      case ComplaintPriority.LOW:
        return "bg-green-100 text-green-800";
      default:
        return "bg-gray-100 text-gray-800";
    }
  };

  const getStatusIcon = (status: ComplaintStatus) => {
    switch (status) {
      case ComplaintStatus.CREATED:
        return <Clock className="h-5 w-5" />;
      case ComplaintStatus.ASSIGNED:
        return <Clock className="h-5 w-5" />;
      case ComplaintStatus.IN_PROGRESS:
        return <AlertCircle className="h-5 w-5" />;
      case ComplaintStatus.BLOCKED:
        return <AlertCircle className="h-5 w-5" />;
      case ComplaintStatus.RESOLVED:
        return <CheckCircle className="h-5 w-5" />;
      case ComplaintStatus.CLOSED:
        return <XCircle className="h-5 w-5" />;
      default:
        return <Clock className="h-5 w-5" />;
    }
  };

  const handleEdit = () => {
    setIsEditModalOpen(true);
  };

  const handleUpdateComplaint = (updatedComplaint: Complaint) => {
    setComplaint(updatedComplaint);
    setIsEditModalOpen(false);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error || !complaint) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600">{error || "Complaint not found"}</p>
        <button
          onClick={() => navigate(-1)}
          className="mt-4 text-blue-600 hover:text-blue-800"
        >
          Go Back
        </button>
      </div>
    );
  }

  const canEdit =
    user?.role === "DISTRICT_COMMISSIONER" ||
    complaint.createdById === user?.id;

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center space-x-4">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center space-x-2 text-gray-600 hover:text-gray-800"
        >
          <ArrowLeft className="h-5 w-5" />
          <span>Back</span>
        </button>
        <div className="flex-1">
          <h1 className="text-2xl font-bold text-gray-900">
            Complaint #{complaint.complaintNumber}
          </h1>
          <p className="text-gray-600">
            Submitted on {new Date(complaint.createdAt).toLocaleDateString()}
          </p>
        </div>
        {canEdit && (
          <button
            onClick={handleEdit}
            className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
          >
            <Edit className="h-4 w-4" />
            <span>Edit</span>
          </button>
        )}
      </div>

      {/* Complaint Details */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="space-y-4">
          {/* Status and Priority Badges */}
          <div className="flex items-center space-x-4">
            <span
              className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(
                complaint.status
              )}`}
            >
              {getStatusIcon(complaint.status)}
              <span className="ml-2">{statusDisplay(complaint.status)}</span>
            </span>
            <span
              className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${getPriorityColor(
                complaint.priority
              )}`}
            >
              {complaint.priority}
            </span>
          </div>

          {/* Subject */}
          <div>
            <h2 className="text-xl font-semibold text-gray-900 mb-2">
              {complaint.subject}
            </h2>
          </div>

          {/* Description */}
          <div>
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              Description
            </h3>
            <p className="text-gray-700 whitespace-pre-wrap">
              {complaint.description}
            </p>
          </div>

          {/* Location */}
          {complaint.location && (
            <div>
              <h3 className="text-lg font-medium text-gray-900 mb-2">
                Location
              </h3>
              <p className="text-gray-700">{complaint.location}</p>
            </div>
          )}

          {/* Department Assignment */}
          {complaint.assignedDepartment && (
            <div>
              <h3 className="text-lg font-medium text-gray-900 mb-2">
                Assigned Department
              </h3>
              <p className="text-gray-700">
                {getDepartmentDisplayName(complaint.assignedDepartment)}
              </p>
              {complaint.departmentRemarks && (
                <p className="text-gray-600 mt-1 italic">
                  {complaint.departmentRemarks}
                </p>
              )}
            </div>
          )}

          {/* Officer Assignment */}
          {complaint.assignedToId && (
            <div>
              <h3 className="text-lg font-medium text-gray-900 mb-2">
                Assigned Officer
              </h3>
              <p className="text-gray-700">{complaint.assignedToId}</p>
              {complaint.assignmentRemarks && (
                <p className="text-gray-600 mt-1 italic">
                  {complaint.assignmentRemarks}
                </p>
              )}
              {complaint.assignedAt && (
                <p className="text-sm text-gray-500 mt-1">
                  Assigned on{" "}
                  {new Date(complaint.assignedAt).toLocaleDateString()}
                </p>
              )}
            </div>
          )}

          {/* Metadata */}
          <div className="border-t pt-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm text-gray-600">
              <div className="flex items-center space-x-2">
                <Calendar className="h-4 w-4" />
                <span>
                  Created: {new Date(complaint.createdAt).toLocaleDateString()}
                </span>
              </div>
              <div className="flex items-center space-x-2">
                <User className="h-4 w-4" />
                <span>Citizen ID: {complaint.citizenId}</span>
              </div>
              {complaint.updatedAt && (
                <div className="flex items-center space-x-2">
                  <Clock className="h-4 w-4" />
                  <span>
                    Last Updated:{" "}
                    {new Date(complaint.updatedAt).toLocaleDateString()}
                  </span>
                </div>
              )}
              {complaint.closedAt && (
                <div className="flex items-center space-x-2">
                  <CheckCircle className="h-4 w-4" />
                  <span>
                    Closed: {new Date(complaint.closedAt).toLocaleDateString()}
                  </span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Comments Section */}
      <CommentSection complaintId={complaint.id} />

      {/* Edit Modal */}
      {isEditModalOpen && (
        <ComplaintEditModal
          complaint={complaint}
          isOpen={isEditModalOpen}
          onClose={() => setIsEditModalOpen(false)}
          onUpdate={handleUpdateComplaint}
        />
      )}
    </div>
  );
};

export default ComplaintDetail;
