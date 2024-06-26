package com.sixb.note.repository;

import com.sixb.note.entity.Folder;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface FolderRepository extends Neo4jRepository<Folder, String>, FolderRepositoryCustom {

	@Query("MATCH (p:Folder)-[:Hierarchy]->(c:Folder) WHERE p.folderId = $folderId AND c.isDeleted = false RETURN c")
	List<Folder> findSubFoldersByFolderId(String folderId);

	@Query("MATCH (u:User {userId: $userId})-[:Join]->(s:Space)-[:Hierarchy*]->(f:Folder) " +
			"WHERE f.isDeleted = true AND f.updatedAt >= $limit " +
			"MATCH (f1:Folder {isDeleted: false})-[:Hierarchy]->(f) " +
			"RETURN f")
	List<Folder> findDeletedFolders(@Param("userId") long userId, LocalDateTime limit);

	@Query("MATCH (f:Folder) WHERE f.folderId = $folderId RETURN f")
	Optional<Folder> findFolderById(@Param("folderId") String folderId);

	@Query("MATCH (u:User {userId: $userId})-[:Like]->(f:Folder) WHERE f.isDeleted = false RETURN f")
	List<Folder> findAllLikedFoldersByUserId(@Param("userId") long userId);

	@Query("MATCH (u:User {userId: $userId})-[:Like]->(f:Folder) WHERE f.folderId IN $folderIds RETURN f.folderId")
	List<String> findLikedFolderIdsByUserId(@Param("userId") long userId, @Param("folderIds") List<String> folderIds);

	@Query("MATCH path=(root:Folder)-[:Hierarchy*]->(child:Folder {folderId: $folderId})\n" +
			"WHERE ALL(n IN nodes(path) WHERE n.isDeleted = false)\n" +
			"  AND NOT (root)<-[:Hierarchy]-(:Folder)\n" +
			"RETURN root.folderId AS parentFolderId\n")
	String findRootFolderByFolderId(@Param("folderId") String folderId);

	@Query("MATCH (u:User {userId: $userId})-[r:Like]->(f:Folder {folderId: $itemId}) DELETE r")
	void deleteLikeFolder(@Param("userId") long userId, @Param("itemId") String itemId);

	@Query("MATCH (f:Folder {folderId: $folderId}) SET f.title = $newTitle RETURN f")
	void updateFolderTitle(String folderId, String newTitle);

	@Query("MATCH (f:Folder {folderId: $folderId})<-[or:Hierarchy]-(of:Folder) " +
			"MATCH (nf:Folder {folderId: $parentFolderId}) " +
			"CREATE (f)<-[nr:Hierarchy]-(nf) " +
			"DELETE or")
	void relocateFolder(String folderId, String parentFolderId);

	@Query("MATCH path=(p:Folder {folderId: $parentFolderId})-[:Hierarchy*]->(c:Folder {folderId: $endFolderId}) " +
			"WHERE ALL(n IN nodes(path) WHERE n.isDeleted = false) " +
			"RETURN nodes(path) AS folders")
	List<Folder> findFoldersBetween(@Param("parentFolderId") String parentFolderId, @Param("endFolderId") String endFolderId);

	@Query("MATCH (f:Folder {folderId: $folderId}) " +
			"OPTIONAL MATCH (f)-[:Hierarchy*]->(s) " +
			"WHERE s.isDeleted = false " +
			"SET f.isDeleted = true, " +
			"    f.updatedAt = $now, " +
			"    s.isDeleted = true, " +
			"    s.updatedAt = $now " +
			"WITH s " +
			"OPTIONAL MATCH (s)-[:NextPage*]->(p:Page) " +
			"WHERE p.isDeleted = false " +
			"SET p.isDeleted = true, " +
			"    p.updatedAt = $now")
	void deleteFolder(String folderId, LocalDateTime now);

	@Query("MATCH (f:Folder {folderId: $folderId}) " +
			"OPTIONAL MATCH (f)-[:Hierarchy*]->(s) " +
			"WHERE s.updatedAt = $deletedAt " +
			"SET f.isDeleted = false, " +
			"    f.updatedAt = $now, " +
			"    s.isDeleted = false, " +
			"    s.updatedAt = $now " +
			"WITH f, s " +
			"OPTIONAL MATCH (s)-[:NextPage*]->(p:Page) " +
			"WHERE p.updatedAt = $deletedAt " +
			"SET p.isDeleted = false, " +
			"    p.updatedAt = $now")
	void recover(String folderId, LocalDateTime deletedAt, LocalDateTime now);

}

