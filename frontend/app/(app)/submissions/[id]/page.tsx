import SubmissionDetailView from "@/components/SubmissionDetailView";

export default async function SubmissionDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <SubmissionDetailView id={id} />;
}
