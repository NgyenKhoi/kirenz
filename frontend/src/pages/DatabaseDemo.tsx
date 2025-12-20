import { Link } from "react-router-dom";
import { ArrowLeft, Database, Users, FileText, MessageSquare } from "lucide-react";
import Header from "@/components/Header";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { useUsers } from "@/hooks/queries/useUserQueries";
import { useUserPosts } from "@/hooks/queries/usePostQueries";
import { Skeleton } from "@/components/ui/skeleton";

const DatabaseDemo = () => {
  const { data: users, isLoading: usersLoading } = useUsers();
  
  // Fetch posts from multiple users to get total count
  const { data: posts1 } = useUserPosts(1);
  const { data: posts2 } = useUserPosts(2);
  const { data: posts3 } = useUserPosts(3);
  
  const totalPosts = (posts1?.length || 0) + (posts2?.length || 0) + (posts3?.length || 0);
  const totalComments = (posts1?.reduce((sum, p) => sum + p.commentsCount, 0) || 0) +
                        (posts2?.reduce((sum, p) => sum + p.commentsCount, 0) || 0) +
                        (posts3?.reduce((sum, p) => sum + p.commentsCount, 0) || 0);

  return (
    <div className="min-h-screen bg-background">
      <Header />
      
      <main className="container mx-auto max-w-4xl px-4 py-6">
        <Link to="/">
          <Button variant="ghost" size="sm" className="mb-4 -ml-2">
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back to feed
          </Button>
        </Link>

        <div className="space-y-6">
          <div>
            <h1 className="text-3xl font-bold text-foreground mb-2">Database Architecture Demo</h1>
            <p className="text-muted-foreground">
              This application demonstrates a hybrid database architecture using PostgreSQL and MongoDB
            </p>
          </div>

          <div className="grid gap-6 md:grid-cols-2">
            {/* PostgreSQL Card */}
            <Card className="border-2 border-blue-500/20">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle className="flex items-center gap-2">
                    <Database className="h-5 w-5 text-blue-500" />
                    PostgreSQL
                  </CardTitle>
                  <Badge variant="outline" className="bg-blue-500/10 text-blue-500 border-blue-500/20">
                    Relational
                  </Badge>
                </div>
                <CardDescription>
                  Stores structured user and profile data
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <h3 className="font-semibold mb-2">Tables</h3>
                  <ul className="space-y-2 text-sm">
                    <li className="flex items-center gap-2">
                      <div className="h-2 w-2 rounded-full bg-blue-500" />
                      <span className="font-mono">users</span>
                      <span className="text-muted-foreground">- Authentication data</span>
                    </li>
                    <li className="flex items-center gap-2">
                      <div className="h-2 w-2 rounded-full bg-blue-500" />
                      <span className="font-mono">profiles</span>
                      <span className="text-muted-foreground">- User information</span>
                    </li>
                  </ul>
                </div>

                <Separator />

                <div>
                  <h3 className="font-semibold mb-2">Statistics</h3>
                  <div className="flex items-center gap-2">
                    <Users className="h-4 w-4 text-blue-500" />
                    {usersLoading ? (
                      <Skeleton className="h-4 w-20" />
                    ) : (
                      <span className="text-2xl font-bold">{users?.length || 0}</span>
                    )}
                    <span className="text-muted-foreground">users</span>
                  </div>
                </div>

                <Separator />

                <div>
                  <h3 className="font-semibold mb-2">Features</h3>
                  <ul className="space-y-1 text-sm text-muted-foreground">
                    <li>✓ ACID compliance</li>
                    <li>✓ Foreign key relationships</li>
                    <li>✓ Data integrity constraints</li>
                    <li>✓ Complex queries with JOINs</li>
                  </ul>
                </div>
              </CardContent>
            </Card>

            {/* MongoDB Card */}
            <Card className="border-2 border-green-500/20">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle className="flex items-center gap-2">
                    <Database className="h-5 w-5 text-green-500" />
                    MongoDB
                  </CardTitle>
                  <Badge variant="outline" className="bg-green-500/10 text-green-500 border-green-500/20">
                    Document
                  </Badge>
                </div>
                <CardDescription>
                  Stores flexible social content data
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <h3 className="font-semibold mb-2">Collections</h3>
                  <ul className="space-y-2 text-sm">
                    <li className="flex items-center gap-2">
                      <div className="h-2 w-2 rounded-full bg-green-500" />
                      <span className="font-mono">posts</span>
                      <span className="text-muted-foreground">- User posts with media</span>
                    </li>
                    <li className="flex items-center gap-2">
                      <div className="h-2 w-2 rounded-full bg-green-500" />
                      <span className="font-mono">comments</span>
                      <span className="text-muted-foreground">- Post comments</span>
                    </li>
                  </ul>
                </div>

                <Separator />

                <div>
                  <h3 className="font-semibold mb-2">Statistics</h3>
                  <div className="space-y-2">
                    <div className="flex items-center gap-2">
                      <FileText className="h-4 w-4 text-green-500" />
                      <span className="text-2xl font-bold">{totalPosts}</span>
                      <span className="text-muted-foreground">posts</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <MessageSquare className="h-4 w-4 text-green-500" />
                      <span className="text-2xl font-bold">{totalComments}</span>
                      <span className="text-muted-foreground">comments</span>
                    </div>
                  </div>
                </div>

                <Separator />

                <div>
                  <h3 className="font-semibold mb-2">Features</h3>
                  <ul className="space-y-1 text-sm text-muted-foreground">
                    <li>✓ Flexible schema</li>
                    <li>✓ Fast read operations</li>
                    <li>✓ Horizontal scalability</li>
                    <li>✓ Embedded documents</li>
                  </ul>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Architecture Explanation */}
          <Card>
            <CardHeader>
              <CardTitle>Why Hybrid Architecture?</CardTitle>
              <CardDescription>
                Combining the strengths of both database types
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid gap-4 md:grid-cols-2">
                <div>
                  <h3 className="font-semibold mb-2 text-blue-500">PostgreSQL for Users</h3>
                  <p className="text-sm text-muted-foreground">
                    User authentication and profile data requires strong consistency, 
                    referential integrity, and complex relationships. PostgreSQL's ACID 
                    properties ensure data reliability for critical user information.
                  </p>
                </div>
                <div>
                  <h3 className="font-semibold mb-2 text-green-500">MongoDB for Content</h3>
                  <p className="text-sm text-muted-foreground">
                    Posts and comments benefit from MongoDB's flexible schema and fast 
                    read operations. The document model naturally represents social content 
                    with nested media arrays and allows for easy scaling.
                  </p>
                </div>
              </div>

              <Separator />

              <div>
                <h3 className="font-semibold mb-2">Data Flow</h3>
                <div className="bg-muted/50 p-4 rounded-lg">
                  <code className="text-sm">
                    <div>1. User creates account → <span className="text-blue-500">PostgreSQL</span> (users table)</div>
                    <div>2. User updates profile → <span className="text-blue-500">PostgreSQL</span> (profiles table)</div>
                    <div>3. User creates post → <span className="text-green-500">MongoDB</span> (posts collection)</div>
                    <div>4. User adds comment → <span className="text-green-500">MongoDB</span> (comments collection)</div>
                    <div>5. Display feed → Fetch from <span className="text-blue-500">PostgreSQL</span> + <span className="text-green-500">MongoDB</span></div>
                  </code>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Technical Implementation */}
          <Card>
            <CardHeader>
              <CardTitle>Technical Implementation</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid gap-4 md:grid-cols-2">
                <div>
                  <h3 className="font-semibold mb-2">Backend</h3>
                  <ul className="space-y-1 text-sm text-muted-foreground">
                    <li>• Spring Data JPA for PostgreSQL</li>
                    <li>• Spring Data MongoDB for MongoDB</li>
                    <li>• MapStruct for DTO mapping</li>
                    <li>• Separate repository interfaces</li>
                    <li>• Service layer coordinates both DBs</li>
                  </ul>
                </div>
                <div>
                  <h3 className="font-semibold mb-2">Frontend</h3>
                  <ul className="space-y-1 text-sm text-muted-foreground">
                    <li>• React Query for data fetching</li>
                    <li>• Unified API client</li>
                    <li>• Automatic cache management</li>
                    <li>• Optimistic updates</li>
                    <li>• Real-time data synchronization</li>
                  </ul>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
};

export default DatabaseDemo;
