package rivalsanalyzerAI.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import rivalsanalyzerAI.Entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByUsername(String username);
}
