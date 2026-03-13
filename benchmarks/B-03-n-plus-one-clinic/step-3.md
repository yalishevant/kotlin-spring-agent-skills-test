## Fix the N+1

Implement the following:

### For the list endpoint (`GET /api/courses`):
Use a **DTO projection** approach:
- Create `CourseSummaryDto` (id, title, category, instructor, lessonCount: Long, enrollmentCount: Long)
- Write a JPQL query: `SELECT new CourseSummaryDto(c.id, c.title, c.category, c.instructor, (SELECT COUNT(l) FROM Lesson l WHERE l.course = c), (SELECT COUNT(e) FROM Enrollment e WHERE e.course = c)) FROM Course c`
- This executes as a SINGLE query (or minimal queries)

### For the detail endpoint (`GET /api/courses/{id}`):
Use **@EntityGraph** to load lessons eagerly in a single JOIN query:
- `@EntityGraph(attributePaths = ["lessons"])` on the findById method
- enrollmentCount is loaded via a separate COUNT query

### Verification:
- Add a query count assertion test: list endpoint must execute <= 3 SQL queries
- Add a query count assertion test: detail endpoint must execute <= 3 SQL queries
- All existing tests still pass
- API response format is unchanged (same JSON structure)

Tip: to count SQL queries in tests, use a `DataSourceProxy` or intercept Hibernate's `StatementInspector`.
