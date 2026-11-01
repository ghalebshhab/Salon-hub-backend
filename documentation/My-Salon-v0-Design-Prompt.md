# v0 Design Prompt — Owner “My Salon” Workspace

Design a production-quality owner-facing **My Salon** page for Salon Hup, a premium salon booking and workforce platform. This is an authenticated operational page, not the public salon profile. Preserve the established Salon Hup plum, rose, cream, and gold visual system, its light/dark themes, editorial typography, rounded cards, subtle borders, restrained shadows, and dark-safe hover treatment. Do not introduce a different product identity.

## Product context

The owner manages one salon through this page. The backend returns: salon name, description, salon phone and email, Jordanian city, validated Google Maps URL and coordinates, opening and closing times, current and maximum employee counts, Cloudinary salon cover URL, approval state, opening state, rejection reason, resubmission count, and remaining resubmissions. The owner can edit details, replace the cover from a device or public Google Drive link, open the public salon page, manage services, employees and schedules, bookings, hiring posts and applications, and delete the salon.

## Required design

- Create a responsive desktop/tablet/mobile page with a cinematic but practical salon cover header.
- Put the cover image in a stable 16:7 frame using `object-fit: cover`; include an accessible change-picture action that opens a compact Device / Google Drive media panel with preview, upload progress, validation, and error states.
- Keep the salon name visually separate from the location. Render the city and Google Maps action on their own line with comfortable spacing, wrapping, a map icon, and an external-link indicator.
- Show approval/opening status without covering important image content.
- Primary actions: **Edit salon**, **View public profile**, and a clearly separated destructive **Delete salon** action.
- Add operational cards for Services, Employees & schedules, Booking requests, Create hiring post, Hiring applications, and Employee work gallery. Each card needs an icon, concise explanation, visible focus state, and dark-safe hover that keeps the card readable.
- Add an at-a-glance panel for working hours, contact information, Google Maps location, and employee capacity.
- Add a team summary that previews active employee names, specialties, experience, assigned services, and safe profile images without exposing private contact data.
- Add a portfolio summary with recent Cloudinary work photos/videos and a route to gallery management.
- For a rejected salon, show a high-priority correction panel containing the exact rejection reason, remaining resubmissions, Edit details, and Resubmit actions. Requested and blocked states need equally clear explanations.
- Include skeleton, empty, Cloudinary-not-configured, upload-progress, upload-error, offline, and confirmation-dialog states.
- Meet WCAG AA contrast, keyboard navigation, semantic landmarks, visible focus, reduced-motion preferences, and 44px minimum touch targets.

## Technical constraints

Return React component code that can be split into feature components and hooks. Do not place HTTP calls in the page component. Use the existing CSS-variable theme (`--bg`, `--card`, `--text`, `--muted`, `--primary`, `--primary-dark`, `--secondary`, `--border`, `--shadow`), Lucide icons, React Router links, shared `btn` classes, and existing loading/toast/dialog components. Do not replace business logic, routes, DTO fields, API endpoints, authentication, or role guards. Do not use mock data in the production component.
