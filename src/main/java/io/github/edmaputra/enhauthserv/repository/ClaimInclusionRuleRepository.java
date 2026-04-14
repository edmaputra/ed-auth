package io.github.edmaputra.enhauthserv.repository;

import io.github.edmaputra.enhauthserv.entity.ClaimInclusionRule;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimInclusionRuleRepository extends JpaRepository<ClaimInclusionRule, String> {

  List<ClaimInclusionRule> findByAttributeKeyIn(Collection<String> attributeKeys);

  boolean existsByAttributeKey(String attributeKey);
}