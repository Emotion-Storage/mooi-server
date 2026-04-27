# Legacy Architecture Notes

AWS 배포 파일과 staging 프로필은 의도적으로 저장소에 남겨둡니다.

이 파일들은 기존 운영 구조를 보여주고, 아래와 같은 전환 흐름을 설명하는 데 도움이 됩니다.

- 기존: AWS 기반 다중 환경 배포
- 개선: 저비용 단일 프로덕션 배포

Neon 전환이 끝나고 문서화가 완료될 때까지 아래 파일은 유지합니다.

- `.github/workflows/deploy-staging-auto.yml`
- `.github/workflows/deploy-staging-manual.yml`
- `.github/workflows/rollback-staging.yml`
- `src/main/resources/application-staging.yml`
- `scripts/deploy.sh`

새 운영 구조가 안정화된 이후에는 아래 중 하나로 정리할 수 있습니다.

- 포트폴리오용 legacy 예시로 그대로 유지
- 별도 `legacy/` 영역으로 이동
- 이후 정리 브랜치에서 제거
