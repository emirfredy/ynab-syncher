# Security Policy

## 🔒 **Supported Versions**

We provide security updates for the following versions:

| Version | Supported        |
| ------- | ---------------- |
| 1.0.x   | ✅ Supported     |
| < 1.0   | ❌ Not supported |

## 🚨 **Reporting a Vulnerability**

We take security vulnerabilities seriously. If you discover a security issue, please follow these guidelines:

### **Private Disclosure**

**DO NOT** create a public GitHub issue for security vulnerabilities.

Instead, please report security issues privately by:

1. **Email**: Send details to `security@personal.co` (if available)
2. **GitHub Security Advisory**: Use GitHub's private vulnerability reporting feature
3. **Direct Contact**: Contact the maintainer directly via their GitHub profile

### **What to Include**

When reporting a security vulnerability, please include:

- **Description**: Clear description of the vulnerability
- **Impact**: Potential impact and attack scenarios
- **Steps to Reproduce**: Detailed steps to reproduce the issue
- **Proof of Concept**: Code or screenshots (if applicable)
- **Suggested Fix**: If you have ideas for fixing the issue
- **Environment**: Java version, OS, and other relevant details

### **Response Timeline**

We aim to respond to security reports within:

- **Initial Response**: 48 hours
- **Status Update**: 1 week
- **Resolution**: 30 days (depending on severity)

## 🛡️ **Security Measures**

### **Code Security**

- **Dependency Scanning**: Automated vulnerability scanning via GitHub Dependabot
- **Static Analysis**: Security-focused code analysis
- **Minimal Dependencies**: Limited external dependencies to reduce attack surface
- **Input Validation**: All external inputs are validated and sanitized

### **API Security**

- **Authentication**: Secure YNAB API token handling
- **Authorization**: Proper access controls
- **Rate Limiting**: Protection against abuse
- **HTTPS Only**: All external communications use HTTPS

### **Build Security**

- **Supply Chain**: Verified dependencies and build reproducibility
- **CI/CD Security**: Secure build pipeline with credential management
- **Code Signing**: Release artifacts are signed (when applicable)

## 🔍 **Security Best Practices**

### **For Users**

- Keep your YNAB API tokens secure and private
- Use environment variables for sensitive configuration
- Keep the application and its dependencies updated
- Monitor application logs for suspicious activity

### **For Developers**

- Follow secure coding practices
- Never commit sensitive data (API keys, passwords, etc.)
- Use parameterized queries to prevent injection attacks
- Implement proper error handling to avoid information leakage
- Validate all inputs at system boundaries

## 🚫 **Known Security Considerations**

### **YNAB API Integration**

- **API Token Storage**: Ensure YNAB API tokens are stored securely
- **Token Rotation**: Regularly rotate API tokens
- **Scope Limitation**: Use minimal required API permissions

### **Financial Data**

- **Data Sensitivity**: Financial transaction data requires special handling
- **Logging**: Avoid logging sensitive financial information
- **Memory Management**: Clear sensitive data from memory when no longer needed

## 📋 **Security Checklist**

For maintainers reviewing security-related changes:

- [ ] Input validation implemented
- [ ] Authentication and authorization checked
- [ ] Sensitive data handling reviewed
- [ ] Error messages don't leak information
- [ ] Dependencies scanned for vulnerabilities
- [ ] Security tests included
- [ ] Documentation updated

## 🔄 **Incident Response**

In case of a security incident:

1. **Immediate Action**: Assess and contain the threat
2. **Investigation**: Determine the scope and impact
3. **Communication**: Notify affected users appropriately
4. **Remediation**: Implement fixes and security patches
5. **Post-Incident**: Review and improve security measures

## 📚 **Security Resources**

### **External Resources**

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Java Security Guidelines](https://www.oracle.com/java/technologies/javase/seccodeguide.html)
- [Spring Security Documentation](https://spring.io/projects/spring-security)

### **Tools and Scanning**

- **Dependency Check**: OWASP Dependency Check
- **Static Analysis**: SpotBugs, SonarQube
- **Container Scanning**: If using Docker containers

## 🙏 **Acknowledgments**

We appreciate responsible disclosure of security vulnerabilities. Security researchers who report vulnerabilities will be:

- Credited in our security advisories (with permission)
- Recognized in release notes
- Listed in our hall of fame (if maintained)

## 📞 **Contact**

For security-related questions or concerns:

- **GitHub**: Create a private security advisory
- **General Security Questions**: Use GitHub Discussions

---

Thank you for helping keep YNAB Syncher secure! 🔒
