import PublicApplyView from "@/components/PublicApplyView";

export default async function ApplyJobPage({ params }: { params: Promise<{ code: string }> }) {
  const { code } = await params;
  return <PublicApplyView code={code} />;
}
