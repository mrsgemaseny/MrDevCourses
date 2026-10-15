-- Migration V71: Add embedding column to student_help_requests for RAG dataset vector search
ALTER TABLE student_help_requests
ADD COLUMN IF NOT EXISTS embedding TEXT;
